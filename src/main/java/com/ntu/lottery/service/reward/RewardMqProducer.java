package com.ntu.lottery.service.reward;

public interface RewardMqProducer {
    void publish(String topic, Object msg);
}
