package com.ntu.lottery.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserAwardOrder {
    private Long id;
    private Long userId;
    private Long activityId;
    private String takeBizId;
    private Long prizeId;
    private String prizeName;
    private Integer prizeType;
    private String state;     // CREATE / SENT
    private LocalDateTime createTime;
}
