package com.ntu.lottery.entity;

import lombok.Data;

@Data
public class ActivityWeightRule {
    private Long id;
    private Long activityId;
    private String ruleCode;
    private String ruleName;
    /**
     * POINTS / LEVEL
     */
    private String ruleType;
    private Integer minValue;
    private Integer maxValue;
    private Integer priority;
}
