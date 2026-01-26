package com.ntu.lottery.service.stock;

import com.ntu.lottery.config.MqTopicProperties;
import com.ntu.lottery.service.stock.msg.StockZeroMsg;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockZeroProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MqTopicProperties topics;

    public StockZeroProducer(RabbitTemplate rabbitTemplate, MqTopicProperties topics) {
        this.rabbitTemplate = rabbitTemplate;
        this.topics = topics;
    }

    public void publish(StockZeroMsg msg) {
        rabbitTemplate.convertAndSend(MqTopicProperties.EXCHANGE, topics.getActivitySkuStockZeroDelay(), msg);
    }
}
