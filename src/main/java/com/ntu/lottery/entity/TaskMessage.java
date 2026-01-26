package com.ntu.lottery.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Local message table for eventual consistency (Task table).
 */
@Data
public class TaskMessage {

    private Long id;

    /** SEND_AWARD / SEND_REBATE / ... */
    private String bizType;

    /** Business unique id for idempotency */
    private String bizId;

    /** Topic name */
    private String topic;

    /** JSON payload */
    private String payload;

    /** CREATE / SUCCESS / FAIL */
    private String state;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private String lastError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
