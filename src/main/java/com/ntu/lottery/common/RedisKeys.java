package com.ntu.lottery.common;

/**
 * Keep all Redis key patterns here to avoid mismatches.
 */
public final class RedisKeys {

    private RedisKeys() {}

    // Distributed lock keys
    public static String assembleLock(Long activityId) {
        return "lottery:assemble:activity:" + activityId;
    }

    // Cached prize config map for an activity
    public static String activityPrizeConfig(Long activityId) {
        return "lottery:activity:" + activityId + ":prize:config";
    }

    // Probability lookup table (shuffled list of prizeId)
    public static String rateTable(Long activityId, String strategyKey) {
        String key = strategyKey == null ? "default" : strategyKey;
        return "lottery:activity:" + activityId + ":rate:table:" + key;
    }

    // Probability table length (range)
    public static String rateTableRange(Long activityId, String strategyKey) {
        String key = strategyKey == null ? "default" : strategyKey;
        return "lottery:activity:" + activityId + ":rate:range:" + key;
    }

    // Draw cost cache (source of truth: DB `activity.draw_cost`)
    public static String drawCost(Long activityId) {
        return "lottery:activity:" + activityId + ":draw:cost";
    }

    // SKU stock counters (atomic)
    public static String skuStockTotal(Long activityId, Long skuId) {
        return "lottery:activity:" + activityId + ":sku:" + skuId + ":stock:total";
    }

    public static String skuStockMonth(Long activityId, Long skuId) {
        return "lottery:activity:" + activityId + ":sku:" + skuId + ":stock:month";
    }

    public static String skuStockDay(Long activityId, Long skuId) {
        return "lottery:activity:" + activityId + ":sku:" + skuId + ":stock:day";
    }

    // Daily draw limit counter per user
    public static String userDailyDrawCount(Long activityId, Long userId, String day) {
        return "lottery:activity:" + activityId + ":user:" + userId + ":draw:" + day;
    }

    // Blacklist set
    public static String blacklistSet() {
        return "lottery:blacklist:users";
    }

    // Distributed lock for SKU stock deduction
    public static String skuStockLock(Long activityId, Long skuId) {
        return "lottery:lock:activity:" + activityId + ":sku:" + skuId;
    }
}
