package com.ntu.lottery.service.reward;

/**
 * Transactional event published in local transaction B.
 * The MQ publish should happen AFTER_COMMIT.
 */
public record TaskCreatedEvent(Long taskId) {
}
