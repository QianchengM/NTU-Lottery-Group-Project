package com.ntu.lottery.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntu.lottery.entity.TaskMessage;
import com.ntu.lottery.mapper.TaskMessageMapper;
import com.ntu.lottery.service.reward.RebateDispatchMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardAfterCommitPublisher {

    private final TaskMessageMapper taskMessageMapper;
    private final RewardMqProducer producer;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        TaskMessage task = taskMessageMapper.selectById(event.taskId());
        if (task == null) {
            return;
        }
        if ("SUCCESS".equalsIgnoreCase(task.getState())) {
            return;
        }

        // Push retry info forward BEFORE publish, to reduce duplicated publish in multi-instance.
        int retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
        taskMessageMapper.touchRetry(task.getId(), computeNextRetryTime(retry));

        try {
            if ("SEND_REBATE".equalsIgnoreCase(task.getBizType())) {
                RebateDispatchMsg msg = objectMapper.readValue(task.getPayload(), RebateDispatchMsg.class);
                msg.setTaskId(task.getId());
                producer.publish(task.getTopic(), msg);
            } else {
                RewardDispatchMsg msg = objectMapper.readValue(task.getPayload(), RewardDispatchMsg.class);
                msg.setTaskId(task.getId());
                producer.publish(task.getTopic(), msg);
            }
            log.info("Published reward message AFTER_COMMIT. taskId={}, topic={}, bizId={}",
                    task.getId(), task.getTopic(), task.getBizId());
        } catch (Exception ex) {
            // If publish fails, mark FAIL and rely on scheduled compensation.
            taskMessageMapper.markFail(task.getId(), ex.getMessage(), LocalDateTime.now().plusSeconds(10));
            log.warn("Publish reward message failed. taskId={}, err={}", task.getId(), ex.getMessage());
        }
    }

    private LocalDateTime computeNextRetryTime(int retryCount) {
        // 1s,2s,4s,8s... capped at 300s
        int sec = (int) Math.min(300, Math.pow(2, Math.max(0, retryCount)));
        return LocalDateTime.now().plusSeconds(sec);
    }
}
