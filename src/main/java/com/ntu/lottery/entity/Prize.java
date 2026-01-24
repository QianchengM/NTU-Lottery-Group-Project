package com.ntu.lottery.entity;

// src/main/java/com/ntu/lottery/entity/Prize.java
import lombok.Data;

@Data
public class Prize {
    private Long id;
    private String name;
    private Integer stock;
    private Integer probability;
    private Integer type; // 0:谢谢惠顾, 1:实物, 2:虚拟券
    private Integer activityId;
    private Integer pointCost;
}