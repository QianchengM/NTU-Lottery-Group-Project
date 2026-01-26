package com.ntu.lottery.service.engine.pre;

import java.util.List;

public class PreRuleChain {
    private final List<PreRule> rules;

    public PreRuleChain(List<PreRule> rules) {
        this.rules = rules;
    }

    public PreRuleResult handle(PreRuleContext ctx) {
        PreRuleResult result = PreRuleResult.pass(null);
        for (PreRule rule : rules) {
            result = rule.evaluate(ctx);
            if (!result.isPass()) {
                return result;
            }
            if (result.getStrategyKey() != null) {
                ctx.setStrategyKey(result.getStrategyKey());
            }
        }
        return result;
    }
}
