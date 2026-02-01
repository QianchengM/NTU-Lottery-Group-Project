package com.ntu.lottery.service.engine.tree;

import com.ntu.lottery.service.dto.PrizeConfig;
import com.ntu.lottery.service.stock.StockService;

public class StockNode extends RuleTreeNode {

    private final StockService stockService;

    public StockNode(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        PrizeConfig prize = ctx.getPrizeConfig();
        if (prize == null) {
            return RuleTreeResult.reject("奖品配置缺失");
        }
        Integer type = prize.getType();
        if (type == null || type == 0) {
            return nextOrPass(ctx);
        }
        boolean ok = stockService.deductRedisStock(ctx.getActivityId(), prize.getSkuId());
        if (!ok) {
            return fallbackOrReject("奖品库存不足", ctx);
        }
        return nextOrPass(ctx);
    }
}
