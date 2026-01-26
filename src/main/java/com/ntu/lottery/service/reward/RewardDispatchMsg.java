package com.ntu.lottery.service.reward;

import lombok.Data;

/**
 * Message body for async reward dispatch.
 */
@Data
public class RewardDispatchMsg {

    private Long taskId;

    private Long userId;
    private Long activityId;
    private Long prizeId;

    /**
     * Idempotency key for external reward system (suggest use awardOrderId).
     */
    private String outBizNo;
}
