package com.ntu.lottery.service.stock;

import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.mapper.ActivitySkuMapper;
import com.ntu.lottery.service.LotteryAssembleService;
import com.ntu.lottery.service.stock.msg.StockZeroMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockZeroConsumer {

    private final ActivitySkuMapper activitySkuMapper;
    private final RedissonClient redissonClient;
    private final LotteryAssembleService assembleService;

    @RabbitListener(queues = "${spring.rabbitmq.topic.activity_sku_stock_zero}")
    public void onStockZero(StockZeroMsg msg) {
        if (msg == null || msg.getActivityId() == null || msg.getSkuId() == null) {
            return;
        }
        try {
            activitySkuMapper.updateStockToZero(msg.getActivityId(), msg.getSkuId(), msg.getLevel());
            // clear Redis counters to avoid invalid透传
            redissonClient.getAtomicLong(RedisKeys.skuStockTotal(msg.getActivityId(), msg.getSkuId())).set(0);
            redissonClient.getAtomicLong(RedisKeys.skuStockMonth(msg.getActivityId(), msg.getSkuId())).set(0);
            redissonClient.getAtomicLong(RedisKeys.skuStockDay(msg.getActivityId(), msg.getSkuId())).set(0);
            // rebuild strategy cache (async stock-zero correction)
            assembleService.assembleActivity(msg.getActivityId());
        } catch (Exception ex) {
            log.warn("Stock zero sync failed. activityId={}, skuId={}, err={}",
                    msg.getActivityId(), msg.getSkuId(), ex.getMessage());
        }
    }
}
