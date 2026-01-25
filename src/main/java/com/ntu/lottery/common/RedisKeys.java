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
    public static String rateTable(Long activityId) {
        return "lottery:activity:" + activityId + ":rate:table";
    }

    // Probability table length (range)
    public static String rateTableRange(Long activityId) {
        return "lottery:activity:" + activityId + ":rate:range";
    }

    // Draw cost cache (source of truth: DB `activity.draw_cost`)
    public static String drawCost(Long activityId) {
        return "lottery:activity:" + activityId + ":draw:cost";
    }

    // Prize stock counter (atomic)
    public static String prizeStock(Long prizeId) {
        return "lottery:prize:" + prizeId + ":stock";
    }
}
