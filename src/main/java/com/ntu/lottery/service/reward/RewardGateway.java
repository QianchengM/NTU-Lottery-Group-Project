package com.ntu.lottery.service.reward;

/**
 * External reward dispatch gateway (HTTP/RPC).
 * Must be idempotent by outBizNo.
 */
public interface RewardGateway {
    void sendAward(RewardDispatchMsg msg);
}
