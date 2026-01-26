package com.ntu.lottery.service.reward;

import com.ntu.lottery.mapper.TaskMessageMapper;
import com.ntu.lottery.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitRebateMqConsumer {

    private final TaskMessageMapper taskMessageMapper;
    private final PointsService pointsService;

    @RabbitListener(queues = "${spring.rabbitmq.topic.send_rebate}")
    public void onSendRebate(RebateDispatchMsg msg) {
        Long taskId = msg.getTaskId();
        try {
            pointsService.addForRebate(msg.getUserId(), msg.getActivityId(), msg.getRebateValue(), msg.getOutBizNo());
            if (taskId != null) {
                taskMessageMapper.markSuccess(taskId);
            }
            log.info("Rebate dispatch success. taskId={}, outBizNo={}", taskId, msg.getOutBizNo());
        } catch (DataIntegrityViolationException dup) {
            if (taskId != null) {
                taskMessageMapper.markSuccess(taskId);
            }
            log.info("Rebate dispatch idempotent. taskId={}, outBizNo={}", taskId, msg.getOutBizNo());
        } catch (Exception ex) {
            LocalDateTime next = LocalDateTime.now().plusSeconds(10);
            if (taskId != null) {
                taskMessageMapper.markFail(taskId, ex.getMessage(), next);
            }
            log.warn("Rebate dispatch fail. taskId={}, err={}", taskId, ex.getMessage());
        }
    }
}
