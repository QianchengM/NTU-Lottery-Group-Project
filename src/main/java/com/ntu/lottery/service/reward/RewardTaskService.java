package com.ntu.lottery.service.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntu.lottery.config.MqTopicProperties;
import com.ntu.lottery.entity.TaskMessage;
import com.ntu.lottery.mapper.TaskMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Create local task (message) in transaction B, and publish event.
 * Real MQ publish will happen AFTER_COMMIT via @TransactionalEventListener.
 */
@Service
@RequiredArgsConstructor
public class RewardTaskService {

    private final TaskMessageMapper taskMessageMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MqTopicProperties topics;
    private final ObjectMapper objectMapper;

    /**
     * Create a task for sending award.
     *
     * @param outBizNo idempotency key (recommend: awardOrderId)
     */
    @Transactional
    public Long createSendAwardTaskAndPublish(Long userId, Long activityId, Long prizeId, String outBizNo) {
        RewardDispatchMsg msg = new RewardDispatchMsg();
        msg.setUserId(userId);
        msg.setActivityId(activityId);
        msg.setPrizeId(prizeId);
        msg.setOutBizNo(outBizNo);

        TaskMessage task = new TaskMessage();
        task.setBizType("SEND_AWARD");
        task.setBizId(outBizNo);
        task.setTopic(topics.getSendAward());
        task.setPayload(toJson(msg));
        task.setState("CREATE");
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now());

        taskMessageMapper.insert(task);

        // Publish transactional event. The listener will publish MQ AFTER_COMMIT.
        eventPublisher.publishEvent(new TaskCreatedEvent(task.getId()));
        return task.getId();
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialize task payload failed", e);
        }
    }
}
