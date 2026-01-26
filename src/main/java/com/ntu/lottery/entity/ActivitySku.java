package com.ntu.lottery.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivitySku {
    private Long id;
    private Long activityId;
    private Long skuId;
    private Integer stockTotal;
    private Integer stockMonth;
    private Integer stockDay;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
