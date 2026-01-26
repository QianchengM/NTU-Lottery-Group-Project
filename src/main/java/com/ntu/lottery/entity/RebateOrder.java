package com.ntu.lottery.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RebateOrder {
    private Long id;
    private Long userId;
    private Long inviterId;
    private Long activityId;
    private String rebateType;
    private Integer rebateValue;
    private String bizId;
    private String state;
    private LocalDateTime createTime;
}
