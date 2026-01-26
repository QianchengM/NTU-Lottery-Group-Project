package com.ntu.lottery.entity;

import lombok.Data;

@Data
public class ActivityWeightPrize {
    private Long id;
    private Long ruleId;
    private Long prizeId;
    private Integer weight;
}
