package com.ntu.lottery.service.engine.pre;

import com.ntu.lottery.service.risk.RiskControlService;
import org.springframework.stereotype.Component;

@Component
public class BlacklistPreRule implements PreRule {

    private final RiskControlService riskControlService;

    public BlacklistPreRule(RiskControlService riskControlService) {
        this.riskControlService = riskControlService;
    }

    @Override
    public PreRuleResult evaluate(PreRuleContext ctx) {
        if (riskControlService.isBlacklisted(ctx.getUserId())) {
            return PreRuleResult.reject("当前账号无法参与抽奖");
        }
        return PreRuleResult.pass(ctx.getStrategyKey());
    }
}
