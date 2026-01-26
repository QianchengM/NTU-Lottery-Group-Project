package com.ntu.lottery.service.engine.pre;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PreRuleChainFactory {

    private final BlacklistPreRule blacklistPreRule;
    private final WeightPreRule weightPreRule;
    private final DefaultPreRule defaultPreRule;

    public PreRuleChainFactory(BlacklistPreRule blacklistPreRule,
                               WeightPreRule weightPreRule,
                               DefaultPreRule defaultPreRule) {
        this.blacklistPreRule = blacklistPreRule;
        this.weightPreRule = weightPreRule;
        this.defaultPreRule = defaultPreRule;
    }

    public PreRuleChain buildChain() {
        return new PreRuleChain(List.of(
                blacklistPreRule,
                weightPreRule,
                defaultPreRule
        ));
    }
}
