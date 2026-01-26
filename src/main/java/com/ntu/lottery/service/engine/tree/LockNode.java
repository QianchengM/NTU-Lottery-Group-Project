package com.ntu.lottery.service.engine.tree;

import com.ntu.lottery.service.limit.DrawLimitService;

public class LockNode implements RuleTreeNode {

    private final DrawLimitService drawLimitService;
    private RuleTreeNode next;

    public LockNode(DrawLimitService drawLimitService) {
        this.drawLimitService = drawLimitService;
    }

    public LockNode next(RuleTreeNode next) {
        this.next = next;
        return this;
    }

    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        int limit = ctx.getDailyLimit() == null ? 0 : ctx.getDailyLimit();
        boolean ok = drawLimitService.tryIncrement(ctx.getActivityId(), ctx.getUserId(), limit);
        if (!ok) {
            return RuleTreeResult.reject("今日参与次数已用完");
        }
        if (next == null) {
            return RuleTreeResult.pass();
        }
        return next.evaluate(ctx);
    }
}
