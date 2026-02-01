package com.ntu.lottery.service.order;

import com.ntu.lottery.entity.UserAwardOrder;
import com.ntu.lottery.entity.UserTakeOrder;
import com.ntu.lottery.mapper.UserAwardOrderMapper;
import com.ntu.lottery.mapper.UserTakeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TakeOrderCompensationJob {

    private final UserTakeOrderMapper userTakeOrderMapper;
    private final UserAwardOrderMapper userAwardOrderMapper;

    /**
     * Scan PROCESSING take orders and ensure they are finalized.
     */
    @Scheduled(fixedDelay = 15000)
    public void scanProcessingOrders() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(2);
        List<UserTakeOrder> orders = userTakeOrderMapper.scanProcessingTimeout(before, 200);
        if (orders == null || orders.isEmpty()) {
            return;
        }
        for (UserTakeOrder order : orders) {
            try {
                UserAwardOrder award = userAwardOrderMapper.selectByTakeBizId(order.getBizId());
                if (award != null) {
                    userTakeOrderMapper.markUsed(order.getBizId());
                    continue;
                }
                log.warn("Take order stuck in PROCESSING without award. bizId={}, userId={}, activityId={}",
                        order.getBizId(), order.getUserId(), order.getActivityId());
            } catch (Exception ex) {
                log.warn("Take order compensation failed. bizId={}, err={}",
                        order.getBizId(), ex.getMessage());
            }
        }
    }
}
