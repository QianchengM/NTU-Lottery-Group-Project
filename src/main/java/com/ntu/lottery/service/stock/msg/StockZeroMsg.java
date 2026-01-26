package com.ntu.lottery.service.stock.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockZeroMsg {
    private Long activityId;
    private Long skuId;
    /**
     * TOTAL / MONTH / DAY
     */
    private String level;
}
