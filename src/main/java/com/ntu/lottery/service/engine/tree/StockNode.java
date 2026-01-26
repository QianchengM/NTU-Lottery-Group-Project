package com.ntu.lottery.service.engine.tree;

import com.ntu.lottery.service.dto.PrizeConfig;
import com.ntu.lottery.service.stock.StockService;

public class StockNode implements RuleTreeNode {

    private final StockService stockService;
    private RuleTreeNode next;
    private RuleTreeNode fallback;

    public StockNode(StockService stockService) {
        this.stockService = stockService;
    }

    public StockNode next(RuleTreeNode next) {
        this.next = next;
        return this;
    }

    public StockNode fallback(RuleTreeNode fallback) {
        this.fallback = fallback;
        return this;
    }

    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        PrizeConfig prize = ctx.getPrizeConfig();
        if (prize == null) {
            return RuleTreeResult.reject("奖品配置缺失");
        }
        Integer type = prize.getType();
        if (type == null || type == 0) {
            return next == null ? RuleTreeResult.pass() : next.evaluate(ctx);
        }
        boolean ok = stockService.deductRedisStock(ctx.getActivityId(), prize.getSkuId());
        if (!ok) {
            if (fallback != null) {
                return fallback.evaluate(ctx);
            }
            return RuleTreeResult.reject("奖品库存不足");
        }
        return next == null ? RuleTreeResult.pass() : next.evaluate(ctx);
    }
}
