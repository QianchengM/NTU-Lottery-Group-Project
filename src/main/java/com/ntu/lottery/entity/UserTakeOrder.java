package com.ntu.lottery.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserTakeOrder {
    private Long id;
    private Long userId;
    private Long activityId;
    private String bizId;     // 参与订单号
    private String state;     // CREATE / USED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
