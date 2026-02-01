package com.ntu.lottery.service.stock;

import com.ntu.lottery.mapper.ActivitySkuMapper;
import com.ntu.lottery.service.stock.msg.StockDeductMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDeductConsumer {

    private final ActivitySkuMapper activitySkuMapper;

    @RabbitListener(queues = "${spring.rabbitmq.topic.activity_sku_stock_deduct}")
    public void onStockDeduct(StockDeductMsg msg) {
        if (msg == null || msg.getActivityId() == null || msg.getSkuId() == null) {
            return;
        }
        try {
            int count = msg.getCount() == null ? 1 : Math.max(1, msg.getCount());
            for (int i = 0; i < count; i++) {
                int rows = activitySkuMapper.deductStock(msg.getActivityId(), msg.getSkuId());
                if (rows <= 0) {
                    log.warn("Stock deduct sync skipped (db no stock). activityId={}, skuId={}",
                            msg.getActivityId(), msg.getSkuId());
                    break;
                }
            }
        } catch (Exception ex) {
            log.warn("Stock deduct sync failed. activityId={}, skuId={}, err={}",
                    msg.getActivityId(), msg.getSkuId(), ex.getMessage());
        }
    }
}
