package com.ntu.lottery.service.reward;

import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.mapper.TaskMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitRewardMqConsumer {

    private final TaskMessageMapper taskMessageMapper;
    private final RewardGateway rewardGateway;
    private final RedissonClient redissonClient;

    @RabbitListener(queues = "${spring.rabbitmq.topic.send_award}")
    public void onSendAward(RewardDispatchMsg msg) {
        Long taskId = msg.getTaskId();
        try {
            if (taskId != null) {
                com.ntu.lottery.entity.TaskMessage task = taskMessageMapper.selectById(taskId);
                if (task != null && "SUCCESS".equalsIgnoreCase(task.getState())) {
                    log.info("Reward dispatch skip (already success). taskId={}, outBizNo={}", taskId, msg.getOutBizNo());
                    return;
                }
            }
            if (msg.getOutBizNo() != null) {
                String key = RedisKeys.rewardDedupKey(msg.getOutBizNo());
                RBucket<String> bucket = redissonClient.getBucket(key);
                boolean first = bucket.trySet("1", 1, TimeUnit.DAYS);
                if (!first) {
                    if (taskId != null) {
                        taskMessageMapper.markSuccess(taskId);
                    }
                    log.info("Reward dispatch idempotent (dedupe). taskId={}, outBizNo={}", taskId, msg.getOutBizNo());
                    return;
                }
            }
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
            if (msg.getOutBizNo() != null) {
                redissonClient.getBucket(RedisKeys.rewardDedupKey(msg.getOutBizNo())).delete();
            }
            log.warn("Reward dispatch fail. taskId={}, err={}", taskId, ex.getMessage());
        }
    }
}
