package com.ntu.lottery.service;

import com.ntu.lottery.common.BusinessException;
import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.entity.ActivitySku;
import com.ntu.lottery.entity.ActivityWeightPrize;
import com.ntu.lottery.entity.ActivityWeightRule;
import com.ntu.lottery.mapper.ActivitySkuMapper;
import com.ntu.lottery.mapper.ActivityWeightPrizeMapper;
import com.ntu.lottery.mapper.ActivityWeightRuleMapper;
import com.ntu.lottery.mapper.PrizeMapper;
import com.ntu.lottery.service.dto.PrizeConfig;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Preheat (assemble) activity prizes into Redis:
 * - prize configs map
 * - prize stock counters
 * - probability lookup table (shuffled)
 */
@Service
public class LotteryAssembleService {

    private final PrizeMapper prizeMapper;
    private final ActivitySkuMapper activitySkuMapper;
    private final ActivityWeightRuleMapper activityWeightRuleMapper;
    private final ActivityWeightPrizeMapper activityWeightPrizeMapper;
    private final RedissonClient redissonClient;

    public LotteryAssembleService(PrizeMapper prizeMapper,
                                  ActivitySkuMapper activitySkuMapper,
                                  ActivityWeightRuleMapper activityWeightRuleMapper,
                                  ActivityWeightPrizeMapper activityWeightPrizeMapper,
                                  RedissonClient redissonClient) {
        this.prizeMapper = prizeMapper;
        this.activitySkuMapper = activitySkuMapper;
        this.activityWeightRuleMapper = activityWeightRuleMapper;
        this.activityWeightPrizeMapper = activityWeightPrizeMapper;
        this.redissonClient = redissonClient;
    }

    /**
     * Idempotent assemble:
     * - guarded by distributed lock
     * - overwrite cache every time (simple + safe)
     */
    public void assembleActivity(Long activityId) {
        if (activityId == null) {
            throw new BusinessException(400, "activityId is required");
        }

        RLock lock = redissonClient.getLock(RedisKeys.assembleLock(activityId));
        boolean locked = false;
        try {
            // wait up to 3s, lease 30s
            locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(429, "Assemble is running, try again later");
            }

            List<Map<String, Object>> rows = prizeMapper.selectByActivityId(activityId);
            if (rows == null || rows.isEmpty()) {
                throw new BusinessException(404, "No prizes configured for activityId=" + activityId);
            }

            // 1) Prize config map (contains skuId)
            Map<Long, PrizeConfig> cfgMap = new HashMap<>();
            for (Map<String, Object> r : rows) {
                PrizeConfig c = toConfig(r);
                cfgMap.put(c.getId(), c);
            }

            // overwrite
            RMap<Long, PrizeConfig> redisCfg = redissonClient.getMap(RedisKeys.activityPrizeConfig(activityId));
            redisCfg.clear();
            redisCfg.putAll(cfgMap);

            // 2) SKU stock counters (atomic)
            List<ActivitySku> skuList = activitySkuMapper.selectByActivityId(activityId);
            if (skuList != null) {
                for (ActivitySku sku : skuList) {
                    long total = sku.getStockTotal() == null ? 0L : Math.max(0, sku.getStockTotal());
                    long month = sku.getStockMonth() == null ? 0L : Math.max(0, sku.getStockMonth());
                    long day = sku.getStockDay() == null ? 0L : Math.max(0, sku.getStockDay());
                    redissonClient.getAtomicLong(RedisKeys.skuStockTotal(activityId, sku.getSkuId())).set(total);
                    redissonClient.getAtomicLong(RedisKeys.skuStockMonth(activityId, sku.getSkuId())).set(month);
                    redissonClient.getAtomicLong(RedisKeys.skuStockDay(activityId, sku.getSkuId())).set(day);
                }
            }

            // 3) Probability lookup table (default strategy)
            ProbabilityTable table = buildProbabilityTable(cfgMap.values(), null);

            // store range
            redissonClient.getBucket(RedisKeys.rateTableRange(activityId, "default")).set(table.range);

            // store list
            RList<Long> rList = redissonClient.getList(RedisKeys.rateTable(activityId, "default"));
            rList.clear();
            rList.addAll(table.indexToPrizeId);

            // 4) Weight rule tables
            List<ActivityWeightRule> rules = activityWeightRuleMapper.selectByActivityId(activityId);
            if (rules != null) {
                for (ActivityWeightRule rule : rules) {
                    List<ActivityWeightPrize> weights = activityWeightPrizeMapper.selectByRuleId(rule.getId());
                    Map<Long, Integer> weightMap = new HashMap<>();
                    if (weights != null) {
                        for (ActivityWeightPrize w : weights) {
                            if (w.getPrizeId() != null && w.getWeight() != null) {
                                weightMap.put(w.getPrizeId(), Math.max(0, w.getWeight()));
                            }
                        }
                    }
                    if (weightMap.isEmpty()) {
                        continue;
                    }
                    ProbabilityTable ruleTable = buildProbabilityTable(cfgMap.values(), weightMap);
                    String strategyKey = "rule:" + rule.getId();
                    redissonClient.getBucket(RedisKeys.rateTableRange(activityId, strategyKey))
                            .set(ruleTable.range);
                    RList<Long> list = redissonClient.getList(RedisKeys.rateTable(activityId, strategyKey));
                    list.clear();
                    list.addAll(ruleTable.indexToPrizeId);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "Assemble interrupted");
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                } catch (Exception ignore) {
                    // ignore unlock failures
                }
            }
        }
    }

    /**
     * Build a shuffled "index -> prizeId" list.
     *
     * Algorithm:
     * - min = smallest positive probability
     * - total = sum(probabilities)
     * - range = ceil(total / min)
     * - each prize contributes ceil(prob / min) slots
     * - then trim/pad to exactly `range`
     * - shuffle
     */
    private ProbabilityTable buildProbabilityTable(Collection<PrizeConfig> prizes, Map<Long, Integer> weights) {
        List<PrizeConfig> valid = prizes.stream()
                .filter(p -> {
                    if (weights == null) {
                        return p.getProbability() != null && p.getProbability() > 0;
                    }
                    Integer w = weights.get(p.getId());
                    return w != null && w > 0;
                })
                .toList();
        if (valid.isEmpty()) {
            throw new BusinessException(500, "Invalid probability config: all probabilities are null/<=0");
        }

        BigDecimal min = null;
        BigDecimal total = BigDecimal.ZERO;
        for (PrizeConfig p : valid) {
            int v = weights == null ? p.getProbability() : weights.get(p.getId());
            BigDecimal prob = BigDecimal.valueOf(v);
            total = total.add(prob);
            if (min == null || prob.compareTo(min) < 0) {
                min = prob;
            }
        }

        if (min == null || min.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(500, "Invalid probability config: min<=0");
        }

        int range = total.divide(min, 0, RoundingMode.UP).intValueExact();
        if (range <= 0) {
            throw new BusinessException(500, "Invalid probability config: range<=0");
        }
        // hard guard to prevent accidental huge memory usage
        if (range > 200_000) {
            throw new BusinessException(500,
                    "Probability range too large (" + range + "). " +
                            "Try increasing min probability or using smaller probability units.");
        }

        List<Long> table = new ArrayList<>(range);
        for (PrizeConfig p : valid) {
            int v = weights == null ? p.getProbability() : weights.get(p.getId());
            BigDecimal prob = BigDecimal.valueOf(v);
            int slots = prob.divide(min, 0, RoundingMode.UP).intValue();
            for (int i = 0; i < slots; i++) {
                table.add(p.getId());
            }
        }

        SecureRandom rnd = new SecureRandom();
        // trim/pad to exact range (keep distribution roughly consistent)
        while (table.size() > range) {
            int idx = rnd.nextInt(table.size());
            table.remove(idx);
        }
        while (table.size() < range) {
            // pad using a random existing element (keeps distribution stable)
            table.add(table.get(rnd.nextInt(table.size())));
        }

        // shuffle to avoid pattern
        Collections.shuffle(table, rnd);
        return new ProbabilityTable(range, table);
    }

    private PrizeConfig toConfig(Map<String, Object> r) {
        PrizeConfig c = new PrizeConfig();
        c.setId(((Number) r.get("id")).longValue());
        c.setActivityId(r.get("activity_id") == null ? null : ((Number) r.get("activity_id")).longValue());
        c.setName((String) r.get("name"));
        c.setType(r.get("type") == null ? 0 : ((Number) r.get("type")).intValue());
        c.setStock(r.get("stock") == null ? 0 : ((Number) r.get("stock")).intValue());
        c.setProbability(r.get("probability") == null ? 0 : ((Number) r.get("probability")).intValue());
        c.setPointCost(r.get("point_cost") == null ? null : ((Number) r.get("point_cost")).intValue());
        c.setSkuId(r.get("sku_id") == null ? null : ((Number) r.get("sku_id")).longValue());
        return c;
    }

    private record ProbabilityTable(int range, List<Long> indexToPrizeId) {}
}
