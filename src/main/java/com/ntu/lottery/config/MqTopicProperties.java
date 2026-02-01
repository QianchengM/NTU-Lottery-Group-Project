package com.ntu.lottery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQ Topic properties.
 *
 * We reuse the existing config namespace: spring.rabbitmq.topic.*
 * Even if the actual MQ implementation is Redis pub/sub (Redisson Topic).
 */
@Data
@ConfigurationProperties(prefix = "spring.rabbitmq.topic")
public class MqTopicProperties {

    public static final String EXCHANGE = "lottery.direct";

    /** invite_bound */
    private String inviteBound;

    /** activity_sku_stock_zero */
    private String activitySkuStockZero;
    /** activity_sku_stock_zero_delay */
    private String activitySkuStockZeroDelay;
    /** activity_sku_stock_deduct */
    private String activitySkuStockDeduct;
    /** activity_sku_stock_deduct_delay */
    private String activitySkuStockDeductDelay;

    /** send_award */
    private String sendAward;

    /** send_rebate */
    private String sendRebate;

    /** credit_adjust_success */
    private String creditAdjustSuccess;
}
