package com.ntu.lottery.service.engine.pre;

import com.ntu.lottery.entity.ActivityWeightRule;
import com.ntu.lottery.mapper.ActivityWeightRuleMapper;
import com.ntu.lottery.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WeightPreRule implements PreRule {

    private final ActivityWeightRuleMapper activityWeightRuleMapper;
    private final UserMapper userMapper;

    public WeightPreRule(ActivityWeightRuleMapper activityWeightRuleMapper, UserMapper userMapper) {
        this.activityWeightRuleMapper = activityWeightRuleMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PreRuleResult evaluate(PreRuleContext ctx) {
        List<ActivityWeightRule> rules = activityWeightRuleMapper.selectByActivityId(ctx.getActivityId());
        if (rules == null || rules.isEmpty()) {
            return PreRuleResult.pass(ctx.getStrategyKey());
        }

        if (ctx.getUserPoints() == null || ctx.getUserLevel() == null) {
            Map<String, Object> info = userMapper.selectPointsAndLevel(ctx.getUserId());
            if (info != null) {
                Object p = info.get("points");
                Object l = info.get("level");
                ctx.setUserPoints(p == null ? 0 : ((Number) p).intValue());
                ctx.setUserLevel(l == null ? 0 : ((Number) l).intValue());
            }
        }

        int points = ctx.getUserPoints() == null ? 0 : ctx.getUserPoints();
        int level = ctx.getUserLevel() == null ? 0 : ctx.getUserLevel();

        for (ActivityWeightRule rule : rules) {
            if (matchRule(rule, points, level)) {
                return PreRuleResult.pass("rule:" + rule.getId());
            }
        }
        return PreRuleResult.pass(ctx.getStrategyKey());
    }

    private boolean matchRule(ActivityWeightRule rule, int points, int level) {
        if (rule == null || rule.getRuleType() == null) return false;
        int min = rule.getMinValue() == null ? 0 : rule.getMinValue();
        int max = rule.getMaxValue() == null ? Integer.MAX_VALUE : rule.getMaxValue();
        return switch (rule.getRuleType().toUpperCase()) {
            case "POINTS" -> points >= min && points <= max;
            case "LEVEL" -> level >= min && level <= max;
            default -> false;
        };
    }
}
