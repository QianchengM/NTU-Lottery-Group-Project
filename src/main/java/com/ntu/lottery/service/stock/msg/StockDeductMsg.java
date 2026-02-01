package com.ntu.lottery.service.stock.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductMsg {
    private Long activityId;
    private Long skuId;
    private Integer count;
}
