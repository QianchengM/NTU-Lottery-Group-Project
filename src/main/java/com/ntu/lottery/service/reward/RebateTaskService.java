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

@Service
@RequiredArgsConstructor
public class RebateTaskService {

    private final TaskMessageMapper taskMessageMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MqTopicProperties topics;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long createSendRebateTaskAndPublish(RebateDispatchMsg msg, String outBizNo) {
        TaskMessage task = new TaskMessage();
        task.setBizType("SEND_REBATE");
        task.setBizId(outBizNo);
        task.setTopic(topics.getSendRebate());
        task.setPayload(toJson(msg));
        task.setState("CREATE");
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now());

        taskMessageMapper.insert(task);
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
