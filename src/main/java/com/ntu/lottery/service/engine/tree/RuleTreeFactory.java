package com.ntu.lottery.service.engine.tree;

import com.ntu.lottery.service.limit.DrawLimitService;
import com.ntu.lottery.service.stock.StockService;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class RuleTreeFactory {

    private final DrawLimitService drawLimitService;
    private final StockService stockService;
    private final RedissonClient redissonClient;

    public RuleTreeFactory(DrawLimitService drawLimitService, StockService stockService, RedissonClient redissonClient) {
        this.drawLimitService = drawLimitService;
        this.stockService = stockService;
        this.redissonClient = redissonClient;
    }

    public RuleTreeNode build() {
        LockNode lock = new LockNode(drawLimitService, redissonClient);
        StockNode stock = new StockNode(stockService);
        EndNode end = new EndNode();
        FallbackNode fallback = new FallbackNode();

        lock.next(stock);
        stock.next(end).fallback(fallback);

        return lock;
    }
}
