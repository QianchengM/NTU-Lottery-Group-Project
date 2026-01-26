package com.ntu.lottery.service.engine.pre;

import lombok.Data;

@Data
public class PreRuleResult {
    private boolean pass;
    private String strategyKey;
    private String message;

    public static PreRuleResult pass(String strategyKey) {
        PreRuleResult r = new PreRuleResult();
        r.setPass(true);
        r.setStrategyKey(strategyKey);
        return r;
    }

    public static PreRuleResult reject(String message) {
        PreRuleResult r = new PreRuleResult();
        r.setPass(false);
        r.setMessage(message);
        return r;
    }
}
