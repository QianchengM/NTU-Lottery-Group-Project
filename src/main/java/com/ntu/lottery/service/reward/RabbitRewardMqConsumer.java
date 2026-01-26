package com.ntu.lottery.service.reward;

import com.ntu.lottery.mapper.TaskMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitRewardMqConsumer {

    private final TaskMessageMapper taskMessageMapper;
    private final RewardGateway rewardGateway;

    @RabbitListener(queues = "${spring.rabbitmq.topic.send_award}")
    public void onSendAward(RewardDispatchMsg msg) {
        Long taskId = msg.getTaskId();
        try {
            rewardGateway.sendAward(msg);
            if (taskId != null) {
                taskMessageMapper.markSuccess(taskId);
            }
            log.info("Reward dispatch success. taskId={}, outBizNo={}", taskId, msg.getOutBizNo());
        } catch (Exception ex) {
            LocalDateTime next = LocalDateTime.now().plusSeconds(10);
            if (taskId != null) {
                taskMessageMapper.markFail(taskId, ex.getMessage(), next);
            }
            log.warn("Reward dispatch fail. taskId={}, err={}", taskId, ex.getMessage());
        }
    }
}
