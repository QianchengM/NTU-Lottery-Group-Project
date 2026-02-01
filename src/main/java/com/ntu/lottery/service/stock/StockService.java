package com.ntu.lottery.service.stock;

import com.ntu.lottery.common.RedisKeys;
import com.ntu.lottery.service.stock.msg.StockDeductMsg;
import com.ntu.lottery.service.stock.msg.StockZeroMsg;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {

    private final RedissonClient redissonClient;
    private final StockZeroProducer stockZeroProducer;
    private final StockDeductProducer stockDeductProducer;

    public StockService(RedissonClient redissonClient,
                        StockZeroProducer stockZeroProducer,
                        StockDeductProducer stockDeductProducer) {
        this.redissonClient = redissonClient;
        this.stockZeroProducer = stockZeroProducer;
        this.stockDeductProducer = stockDeductProducer;
    }

    public boolean deductRedisStock(Long activityId, Long skuId) {
        if (activityId == null || skuId == null) {
            return true;
        }

        String script = """
                local t = tonumber(redis.call('get', KEYS[1]) or '0')
                local m = tonumber(redis.call('get', KEYS[2]) or '0')
                local d = tonumber(redis.call('get', KEYS[3]) or '0')
                if t <= 0 or m <= 0 or d <= 0 then
                    return {-1, t, m, d}
                end
                t = t - 1
                m = m - 1
                d = d - 1
                redis.call('set', KEYS[1], t)
                redis.call('set', KEYS[2], m)
                redis.call('set', KEYS[3], d)
                return {t, m, d}
                """;

        List<Object> res = redissonClient.getScript(StringCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.MULTI,
                        java.util.Arrays.asList(
                                RedisKeys.skuStockTotal(activityId, skuId),
                                RedisKeys.skuStockMonth(activityId, skuId),
                                RedisKeys.skuStockDay(activityId, skuId)
                        ));

        if (res == null || res.isEmpty()) {
            return false;
        }
        long t2 = toLong(res.get(0));
        if (t2 < 0) {
            return false;
        }
        long m2 = toLong(res.get(1));
        long d2 = toLong(res.get(2));

        if (t2 == 0) {
            stockZeroProducer.publish(new StockZeroMsg(activityId, skuId, "TOTAL"));
        }
        if (m2 == 0) {
            stockZeroProducer.publish(new StockZeroMsg(activityId, skuId, "MONTH"));
        }
        if (d2 == 0) {
            stockZeroProducer.publish(new StockZeroMsg(activityId, skuId, "DAY"));
        }

        // Async DB sync via delay queue.
        stockDeductProducer.publish(new StockDeductMsg(activityId, skuId, 1));
        return true;
    }

    public void rollbackRedisStock(Long activityId, Long skuId) {
        if (activityId == null || skuId == null) return;
        redissonClient.getAtomicLong(RedisKeys.skuStockTotal(activityId, skuId)).incrementAndGet();
        redissonClient.getAtomicLong(RedisKeys.skuStockMonth(activityId, skuId)).incrementAndGet();
        redissonClient.getAtomicLong(RedisKeys.skuStockDay(activityId, skuId)).incrementAndGet();
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return Long.parseLong(val.toString());
    }
}
