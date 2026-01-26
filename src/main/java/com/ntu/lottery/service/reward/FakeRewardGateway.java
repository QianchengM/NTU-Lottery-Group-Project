package com.ntu.lottery.service.reward;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Demo implementation. Replace with real external API call.
 */
@Slf4j
@Component
public class FakeRewardGateway implements RewardGateway {

    @Override
    public void sendAward(RewardDispatchMsg msg) {
        // TODO: call external reward system, ensure idempotency by outBizNo
        log.info("FAKE sendAward OK. taskId={}, outBizNo={}, userId={}, prizeId={}",
                msg.getTaskId(), msg.getOutBizNo(), msg.getUserId(), msg.getPrizeId());
    }
}
