package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.mapper.ActivityMapper;
import com.ntu.lottery.mapper.PrizeMapper;
import com.ntu.lottery.mapper.RecordMapper;
import com.ntu.lottery.service.dto.PrizeConfig;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// src/main/java/com/ntu/lottery/service/LotteryService.java
@Service
public class LotteryService {
    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private LotteryAssembleService assembleService;

    @Autowired
    private PointsService pointsService;


    public List<Map<String, Object>> listPrizes(Long activityId) {
        if (activityId == null) {
            return prizeMapper.selectAll();
        }
        return prizeMapper.selectByActivityId(activityId);
    }

    @Transactional // 保证抽奖和记录保存同步成功或失败
    public String executeDraw(Long userId, Long activityId) {
        if (userId == null || activityId == null) {
            throw new BusinessException(400, "userId/activityId is required");
        }

        // 0) Deduct points (DB source of truth) + write ledger
        int cost = getDrawCost(activityId);
        pointsService.deductForDraw(userId, activityId, cost);

        // 1) Ensure cache is ready (assemble-on-demand)
        ensureAssembled(activityId);

        // 2) Pick from probability lookup table (O(1))
        Integer range = getRange(activityId);
        if (range == null || range <= 0) {
            throw new BusinessException(500, "Strategy cache missing: range");
        }

        RList<Long> table = redissonClient.getList(RedisKeys.rateTable(activityId));
        if (table.isEmpty()) {
            throw new BusinessException(500, "Strategy cache missing: rate table");
        }

        RMap<Long, PrizeConfig> cfgMap = redissonClient.getMap(RedisKeys.activityPrizeConfig(activityId));

        // 3) Draw with small retry: avoid returning "no stock" too often when one prize is exhausted
        final int maxRetry = 3;
        for (int attempt = 0; attempt < maxRetry; attempt++) {
            int idx = ThreadLocalRandom.current().nextInt(range);
            Long prizeId = table.get(idx);
            if (prizeId == null) {
                continue;
            }

            PrizeConfig cfg = cfgMap.get(prizeId);
            if (cfg == null) {
                // extremely rare: cache inconsistency
                continue;
            }

            String prizeName = cfg.getName();
            int type = cfg.getType() == null ? 0 : cfg.getType();

            // Thanks / no-stock prize
            if (type == 0) {
                saveRecord(userId, activityId, prizeName, type);
                return "很遗憾: " + prizeName;
            }

            // 4) Deduct stock in Redis first (fast-path, protects DB)
            boolean ok = deductRedisStock(prizeId);
            if (!ok) {
                // retry another index
                continue;
            }

            // 5) Persist to DB (simple consistency)
            int rows = prizeMapper.deductStock(prizeId);
            if (rows <= 0) {
                // DB says no stock: compensate Redis
                redissonClient.getAtomicLong(RedisKeys.prizeStock(prizeId)).incrementAndGet();
                continue;
            }

            saveRecord(userId, activityId, prizeName, type);
            return "恭喜你赢得: " + prizeName;
        }

        return "手慢了，奖品已领完";
    }

    private void ensureAssembled(Long activityId) {
        RBucket<Integer> rangeBucket = redissonClient.getBucket(RedisKeys.rateTableRange(activityId));
        Integer range = rangeBucket.get();
        if (range == null || range <= 0) {
            // assemble-on-demand, guarded by distributed lock inside
            assembleService.assembleActivity(activityId);
        }
    }

    private Integer getRange(Long activityId) {
        return (Integer) redissonClient.getBucket(RedisKeys.rateTableRange(activityId)).get();
    }

    /**
     * Get per-activity draw cost from Redis cache (fallback to DB).
     * DB is the source of truth.
     */
    private int getDrawCost(Long activityId) {
        RBucket<Integer> bucket = redissonClient.getBucket(RedisKeys.drawCost(activityId));
        Integer cached = bucket.get();
        if (cached != null) {
            return Math.max(0, cached);
        }
        Integer db = activityMapper.selectDrawCost(activityId);
        int cost = db == null ? 0 : Math.max(0, db);
        // Cache for a short time to reduce DB pressure during high QPS
        bucket.set(cost, 10, TimeUnit.MINUTES);
        return cost;
    }

    /**
     * CAS loop to prevent stock from going negative.
     */
    private boolean deductRedisStock(Long prizeId) {
        RAtomicLong counter = redissonClient.getAtomicLong(RedisKeys.prizeStock(prizeId));
        for (int i = 0; i < 20; i++) {
            long cur = counter.get();
            if (cur <= 0) return false;
            if (counter.compareAndSet(cur, cur - 1)) return true;
        }
        // too much contention: treat as fail-fast
        return false;
    }

    private void saveRecord(Long userId, Long activityId, String prizeName, int type) {
        recordMapper.insertRecord(userId, activityId, prizeName, type);
    }
}