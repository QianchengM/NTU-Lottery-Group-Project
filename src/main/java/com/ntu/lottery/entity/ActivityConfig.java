package com.ntu.lottery.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityConfig {
    private Long id;
    private Integer status;
    private Integer drawCost;
    private Integer dailyDrawLimit;
    private Long fallbackPrizeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
