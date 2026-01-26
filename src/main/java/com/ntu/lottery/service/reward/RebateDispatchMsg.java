package com.ntu.lottery.service.reward;

import lombok.Data;

@Data
public class RebateDispatchMsg {
    private Long taskId;
    private Long userId;
    private Long activityId;
    private String rebateType;
    private Integer rebateValue;
    private String outBizNo;
}
