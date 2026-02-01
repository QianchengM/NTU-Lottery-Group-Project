package com.ntu.lottery.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MqTopicProperties.class)
public class MqTopicConfig {

    @Bean
    public DirectExchange lotteryExchange() {
        return new DirectExchange(MqTopicProperties.EXCHANGE, true, false);
    }

    @Bean
    public Queue sendAwardQueue(MqTopicProperties topics) {
        return new Queue(topics.getSendAward(), true);
    }

    @Bean
    public Queue sendRebateQueue(MqTopicProperties topics) {
        return new Queue(topics.getSendRebate(), true);
    }

    @Bean
    public Queue activitySkuStockZeroQueue(MqTopicProperties topics) {
        return new Queue(topics.getActivitySkuStockZero(), true);
    }

    @Bean
    public Queue activitySkuStockZeroDelayQueue(MqTopicProperties topics) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 5000);
        args.put("x-dead-letter-exchange", MqTopicProperties.EXCHANGE);
        args.put("x-dead-letter-routing-key", topics.getActivitySkuStockZero());
        return new Queue(topics.getActivitySkuStockZeroDelay(), true, false, false, args);
    }

    @Bean
    public Queue activitySkuStockDeductQueue(MqTopicProperties topics) {
        return new Queue(topics.getActivitySkuStockDeduct(), true);
    }

    @Bean
    public Queue activitySkuStockDeductDelayQueue(MqTopicProperties topics) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 2000);
        args.put("x-dead-letter-exchange", MqTopicProperties.EXCHANGE);
        args.put("x-dead-letter-routing-key", topics.getActivitySkuStockDeduct());
        return new Queue(topics.getActivitySkuStockDeductDelay(), true, false, false, args);
    }

    @Bean
    public Binding sendAwardBinding(DirectExchange lotteryExchange, Queue sendAwardQueue, MqTopicProperties topics) {
        return BindingBuilder.bind(sendAwardQueue).to(lotteryExchange).with(topics.getSendAward());
    }

    @Bean
    public Binding sendRebateBinding(DirectExchange lotteryExchange, Queue sendRebateQueue, MqTopicProperties topics) {
        return BindingBuilder.bind(sendRebateQueue).to(lotteryExchange).with(topics.getSendRebate());
    }

    @Bean
    public Binding activitySkuStockZeroBinding(DirectExchange lotteryExchange,
                                               Queue activitySkuStockZeroQueue,
                                               MqTopicProperties topics) {
        return BindingBuilder.bind(activitySkuStockZeroQueue).to(lotteryExchange).with(topics.getActivitySkuStockZero());
    }

    @Bean
    public Binding activitySkuStockZeroDelayBinding(DirectExchange lotteryExchange,
                                                    Queue activitySkuStockZeroDelayQueue,
                                                    MqTopicProperties topics) {
        return BindingBuilder.bind(activitySkuStockZeroDelayQueue)
                .to(lotteryExchange)
                .with(topics.getActivitySkuStockZeroDelay());
    }

    @Bean
    public Binding activitySkuStockDeductBinding(DirectExchange lotteryExchange,
                                                 Queue activitySkuStockDeductQueue,
                                                 MqTopicProperties topics) {
        return BindingBuilder.bind(activitySkuStockDeductQueue)
                .to(lotteryExchange)
                .with(topics.getActivitySkuStockDeduct());
    }

    @Bean
    public Binding activitySkuStockDeductDelayBinding(DirectExchange lotteryExchange,
                                                      Queue activitySkuStockDeductDelayQueue,
                                                      MqTopicProperties topics) {
        return BindingBuilder.bind(activitySkuStockDeductDelayQueue)
                .to(lotteryExchange)
                .with(topics.getActivitySkuStockDeductDelay());
    }
}
