package com.ntu.lottery.service.engine.pre;

public interface PreRule {
    PreRuleResult evaluate(PreRuleContext ctx);
}
