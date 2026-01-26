package com.ntu.lottery.service.engine.tree;

import com.ntu.lottery.service.dto.PrizeConfig;
import lombok.Data;

@Data
public class RuleTreeContext {
    private Long userId;
    private Long activityId;
    private PrizeConfig prizeConfig;
    private Long fallbackPrizeId;
    private Integer dailyLimit;
}
