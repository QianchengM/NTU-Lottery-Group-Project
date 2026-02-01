package com.ntu.lottery.service.stock;

import com.ntu.lottery.config.MqTopicProperties;
import com.ntu.lottery.service.stock.msg.StockDeductMsg;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockDeductProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MqTopicProperties topics;

    public StockDeductProducer(RabbitTemplate rabbitTemplate, MqTopicProperties topics) {
        this.rabbitTemplate = rabbitTemplate;
        this.topics = topics;
    }

    public void publish(StockDeductMsg msg) {
        rabbitTemplate.convertAndSend(MqTopicProperties.EXCHANGE, topics.getActivitySkuStockDeductDelay(), msg);
    }
}
