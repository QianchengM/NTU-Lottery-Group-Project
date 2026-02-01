package com.ntu.lottery.service.engine.tree;

import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.service.dto.PrizeConfig;
import com.ntu.lottery.service.limit.DrawLimitService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class LockNode extends RuleTreeNode {

    private final DrawLimitService drawLimitService;
    private final RedissonClient redissonClient;

    public LockNode(DrawLimitService drawLimitService, RedissonClient redissonClient) {
        this.drawLimitService = drawLimitService;
        this.redissonClient = redissonClient;
    }

    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        int limit = ctx.getDailyLimit() == null ? 0 : ctx.getDailyLimit();
        boolean ok = drawLimitService.tryIncrement(ctx.getActivityId(), ctx.getUserId(), limit);
        if (!ok) {
            return RuleTreeResult.reject("今日参与次数已用完");
        }

        if (ctx.isPrizeLockRequired()) {
            PrizeConfig prize = ctx.getPrizeConfig();
            if (prize != null && prize.getId() != null) {
                int ttl = ctx.getPrizeLockSeconds() == null ? 5 : Math.max(1, ctx.getPrizeLockSeconds());
                String key = RedisKeys.prizeUserLock(ctx.getActivityId(), ctx.getUserId(), prize.getId());
                RBucket<String> bucket = redissonClient.getBucket(key);
                boolean locked = bucket.trySet("1", ttl, TimeUnit.SECONDS);
                if (!locked) {
                    return RuleTreeResult.reject("奖品已被占用，请稍后再试");
                }
            }
        }

        return nextOrPass(ctx);
    }
}
