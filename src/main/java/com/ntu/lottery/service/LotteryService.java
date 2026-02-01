package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.entity.ActivityConfig;
import com.ntu.lottery.mapper.ActivityMapper;
import com.ntu.lottery.mapper.PrizeMapper;
import com.ntu.lottery.mapper.RecordMapper;
import com.ntu.lottery.mapper.UserTakeOrderMapper;
import com.ntu.lottery.service.engine.pre.PreRuleChain;
import com.ntu.lottery.service.engine.pre.PreRuleChainFactory;
import com.ntu.lottery.service.engine.pre.PreRuleContext;
import com.ntu.lottery.service.engine.pre.PreRuleResult;
import com.ntu.lottery.service.engine.tree.RuleTreeContext;
import com.ntu.lottery.service.engine.tree.RuleTreeEngine;
import com.ntu.lottery.service.engine.tree.RuleTreeResult;
import com.ntu.lottery.service.dto.PrizeConfig;
import com.ntu.lottery.service.order.TakeOrderService;
import com.ntu.lottery.service.stock.StockService;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    private PreRuleChainFactory preRuleChainFactory;

    @Autowired
    private RuleTreeEngine ruleTreeEngine;

    @Autowired
    private TakeOrderService takeOrderService;

    @Autowired
    private AwardOrderService awardOrderService;

    @Autowired
    private StockService stockService;

    @Autowired
    private UserTakeOrderMapper userTakeOrderMapper;

    public List<Map<String, Object>> listPrizes(Long activityId) {
        if (activityId == null) {
            return prizeMapper.selectAll();
        }
        return prizeMapper.selectByActivityId(activityId);
    }

    public String executeDraw(Long userId, Long activityId) {
        if (userId == null || activityId == null) {
            throw new BusinessException(400, "userId/activityId is required");
        }

        // 0) Pre-chain checks: blacklist + weight rule + default
        PreRuleChain chain = preRuleChainFactory.buildChain();
        PreRuleContext preCtx = new PreRuleContext();
        preCtx.setUserId(userId);
        preCtx.setActivityId(activityId);
        PreRuleResult pre = chain.handle(preCtx);
        if (!pre.isPass()) {
            return pre.getMessage();
        }
        String strategyKey = pre.getStrategyKey();

        // 1) Ensure cache is ready (assemble-on-demand)
        ensureAssembled(activityId);

        RMap<Long, PrizeConfig> cfgMap = redissonClient.getMap(RedisKeys.activityPrizeConfig(activityId));
        PrizeConfig prize = drawPrize(activityId, strategyKey, cfgMap);
        if (prize == null) {
            return "抽奖失败，请稍后再试";
        }

        // 2) Rule tree: lock + stock (fallback on no-stock)
        ActivityConfig activity = activityMapper.selectConfig(activityId);
        Long fallbackPrizeId = activity == null ? null : activity.getFallbackPrizeId();
        if (fallbackPrizeId == null) {
            fallbackPrizeId = findThanksPrizeId(cfgMap);
        }

        RuleTreeContext treeCtx = new RuleTreeContext();
        treeCtx.setUserId(userId);
        treeCtx.setActivityId(activityId);
        treeCtx.setPrizeConfig(prize);
        treeCtx.setFallbackPrizeId(fallbackPrizeId);
        treeCtx.setDailyLimit(activity == null ? 0 : activity.getDailyDrawLimit());
        treeCtx.setPrizeLockRequired(prize.getType() != null && prize.getType() != 0);
        treeCtx.setPrizeLockSeconds(5);

        RuleTreeResult treeResult = ruleTreeEngine.evaluate(treeCtx);
        if (treeResult.getStatus() == RuleTreeResult.Status.REJECT) {
            return treeResult.getMessage();
        }
        if (treeResult.getStatus() == RuleTreeResult.Status.FALLBACK) {
            PrizeConfig fb = cfgMap.get(treeResult.getFallbackPrizeId());
            if (fb != null) {
                prize = fb;
            }
        }

        // 3) Transaction A: create take order + deduct points (stock already pre-deducted in Redis)
        int cost = getDrawCost(activityId);
        String takeBizId;
        try {
            Long skuId = (prize.getType() == null || prize.getType() == 0) ? null : prize.getSkuId();
            takeBizId = takeOrderService.createTakeOrder(userId, activityId, skuId, cost);
        } catch (BusinessException ex) {
            if (prize.getSkuId() != null) {
                stockService.rollbackRedisStock(activityId, prize.getSkuId());
            }
            throw ex;
        }

        // 4) Transaction B: save award order + task
        if (prize.getType() != null && prize.getType() != 0) {
            awardOrderService.saveAwardAndMarkUsed(userId, activityId, prize.getId(),
                    prize.getName(), prize.getType(), takeBizId);
        } else {
            userTakeOrderMapper.markUsed(takeBizId);
        }

        saveRecord(userId, activityId, prize.getName(), prize.getType() == null ? 0 : prize.getType());
        if (prize.getType() == null || prize.getType() == 0) {
            return "很遗憾: " + prize.getName();
        }
        return "恭喜你赢得: " + prize.getName();
    }

    private void ensureAssembled(Long activityId) {
        RBucket<Integer> rangeBucket = redissonClient.getBucket(RedisKeys.rateTableRange(activityId, "default"));
        Integer range = rangeBucket.get();
        if (range == null || range <= 0) {
            // assemble-on-demand, guarded by distributed lock inside
            assembleService.assembleActivity(activityId);
        }
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

    private void saveRecord(Long userId, Long activityId, String prizeName, int type) {
        recordMapper.insertRecord(userId, activityId, prizeName, type);
    }

    private PrizeConfig drawPrize(Long activityId, String strategyKey, RMap<Long, PrizeConfig> cfgMap) {
        String key = strategyKey == null ? "default" : strategyKey;
        Integer range = (Integer) redissonClient.getBucket(RedisKeys.rateTableRange(activityId, key)).get();
        if (range == null || range <= 0) {
            key = "default";
            range = (Integer) redissonClient.getBucket(RedisKeys.rateTableRange(activityId, key)).get();
        }
        if (range == null || range <= 0) {
            throw new BusinessException(500, "Strategy cache missing: range");
        }

        RMap<Integer, Long> table = redissonClient.getMap(RedisKeys.rateTableMap(activityId, key));
        if (table.isEmpty()) {
            throw new BusinessException(500, "Strategy cache missing: rate map");
        }

        final int maxRetry = 5;
        for (int attempt = 0; attempt < maxRetry; attempt++) {
            int idx = ThreadLocalRandom.current().nextInt(range) + 1;
            Long prizeId = table.get(idx);
            if (prizeId == null) continue;
            PrizeConfig cfg = cfgMap.get(prizeId);
            if (cfg != null) return cfg;
        }
        return null;
    }

    private Long findThanksPrizeId(RMap<Long, PrizeConfig> cfgMap) {
        for (PrizeConfig cfg : cfgMap.values()) {
            if (cfg.getType() == null || cfg.getType() == 0) {
                return cfg.getId();
            }
        }
        return null;
    }
}
