package com.ntu.lottery.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskBlacklist {
    private Long id;
    private Long userId;
    private String reason;
    private LocalDateTime createTime;
}
