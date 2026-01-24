package com.ntu.lottery.common;

/**
 * For predictable business rule failures (e.g. out of stock, insufficient points).
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
