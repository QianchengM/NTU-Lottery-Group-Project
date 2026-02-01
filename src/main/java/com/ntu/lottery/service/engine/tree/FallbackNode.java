package com.ntu.lottery.service.engine.tree;

public class FallbackNode extends RuleTreeNode {
    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        return RuleTreeResult.fallback(ctx.getFallbackPrizeId());
    }
}
