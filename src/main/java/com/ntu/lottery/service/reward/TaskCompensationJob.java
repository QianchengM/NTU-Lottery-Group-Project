package com.ntu.lottery.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntu.lottery.entity.TaskMessage;
import com.ntu.lottery.mapper.TaskMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to resend CREATE/FAIL tasks for eventual consistency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCompensationJob {

    private final TaskMessageMapper taskMessageMapper;
    private final RewardMqProducer producer;
    private final ObjectMapper objectMapper;

    /**
     * Scan and retry tasks every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void retryFailedTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<TaskMessage> tasks = taskMessageMapper.scanRetryTasks("CREATE", "FAIL", now, 200);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (TaskMessage task : tasks) {
            if ("SUCCESS".equalsIgnoreCase(task.getState())) {
                continue;
            }

            try {
                Object msg;
                if ("SEND_REBATE".equalsIgnoreCase(task.getBizType())) {
                    RebateDispatchMsg rebate = objectMapper.readValue(task.getPayload(), RebateDispatchMsg.class);
                    rebate.setTaskId(task.getId());
                    msg = rebate;
                } else {
                    RewardDispatchMsg award = objectMapper.readValue(task.getPayload(), RewardDispatchMsg.class);
                    award.setTaskId(task.getId());
                    msg = award;
                }

                // Increase retry counter and push next retry time forward.
                taskMessageMapper.touchRetry(task.getId(), now.plusSeconds(10));

                producer.publish(task.getTopic(), msg);
                log.info("Compensation republish ok. taskId={}, bizType={}, bizId={}",
                        task.getId(), task.getBizType(), task.getBizId());
            } catch (Exception ex) {
                // Poison message / publish failed
                taskMessageMapper.markFail(task.getId(), ex.getMessage(), now.plusSeconds(30));
                log.warn("Compensation republish failed. taskId={}, err={}", task.getId(), ex.getMessage());
            }
        }
    }
}
