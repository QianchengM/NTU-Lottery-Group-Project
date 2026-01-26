package com.ntu.lottery.service.stock;

import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.service.stock.msg.StockZeroMsg;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    private final RedissonClient redissonClient;
    private final StockZeroProducer stockZeroProducer;

    public StockService(RedissonClient redissonClient, StockZeroProducer stockZeroProducer) {
        this.redissonClient = redissonClient;
        this.stockZeroProducer = stockZeroProducer;
    }

    public boolean deductRedisStock(Long activityId, Long skuId) {
        if (activityId == null || skuId == null) {
            return true;
        }

        RLock lock = redissonClient.getLock(RedisKeys.skuStockLock(activityId, skuId));
        boolean locked = false;
        try {
            locked = lock.tryLock(100, 3_000, TimeUnit.MILLISECONDS);
            if (!locked) {
                return false;
            }

            RAtomicLong total = redissonClient.getAtomicLong(RedisKeys.skuStockTotal(activityId, skuId));
            RAtomicLong month = redissonClient.getAtomicLong(RedisKeys.skuStockMonth(activityId, skuId));
            RAtomicLong day = redissonClient.getAtomicLong(RedisKeys.skuStockDay(activityId, skuId));

            long t = total.get();
            long m = month.get();
            long d = day.get();
            if (t <= 0 || m <= 0 || d <= 0) {
                return false;
            }

            long t2 = total.decrementAndGet();
            long m2 = month.decrementAndGet();
            long d2 = day.decrementAndGet();

            if (t2 == 0) {
                stockZeroProducer.publish(new StockZeroMsg(activityId, skuId, "TOTAL"));
            }
            if (m2 == 0) {
                stockZeroProducer.publish(new StockZeroMsg(activityId, skuId, "MONTH"));
            }
            if (d2 == 0) {
                stockZeroProducer.publish(new StockZeroMsg(activityId, skuId, "DAY"));
            }

            return t2 >= 0 && m2 >= 0 && d2 >= 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void rollbackRedisStock(Long activityId, Long skuId) {
        if (activityId == null || skuId == null) return;
        redissonClient.getAtomicLong(RedisKeys.skuStockTotal(activityId, skuId)).incrementAndGet();
        redissonClient.getAtomicLong(RedisKeys.skuStockMonth(activityId, skuId)).incrementAndGet();
        redissonClient.getAtomicLong(RedisKeys.skuStockDay(activityId, skuId)).incrementAndGet();
    }
}
