package com.ntu.lottery.service.reward;

import com.ntu.lottery.config.MqTopicProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitRewardMqProducer implements RewardMqProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitRewardMqProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String topic, Object msg) {
        rabbitTemplate.convertAndSend(MqTopicProperties.EXCHANGE, topic, msg);
    }
}
