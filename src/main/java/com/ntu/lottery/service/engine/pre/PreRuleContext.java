package com.ntu.lottery.service.engine.pre;

import lombok.Data;

@Data
public class PreRuleContext {
    private Long userId;
    private Long activityId;
    private Integer userPoints;
    private Integer userLevel;
    private String strategyKey;
}
