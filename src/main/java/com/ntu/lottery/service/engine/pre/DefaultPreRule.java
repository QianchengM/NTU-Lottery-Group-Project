package com.ntu.lottery.service.engine.pre;

import org.springframework.stereotype.Component;

@Component
public class DefaultPreRule implements PreRule {
    @Override
    public PreRuleResult evaluate(PreRuleContext ctx) {
        String strategyKey = ctx.getStrategyKey();
        if (strategyKey == null || strategyKey.isBlank()) {
            strategyKey = "default";
        }
        return PreRuleResult.pass(strategyKey);
    }
}
