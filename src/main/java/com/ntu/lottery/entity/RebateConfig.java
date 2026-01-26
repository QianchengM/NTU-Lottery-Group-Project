package com.ntu.lottery.entity;

import lombok.Data;

@Data
public class RebateConfig {
    private Long id;
    private Long activityId;
    private String rebateType;
    private Integer rebateValue;
    private Integer status;
}
