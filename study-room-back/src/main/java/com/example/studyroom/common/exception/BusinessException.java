package com.example.studyroom.common.exception;

/**
 * 通用业务异常
 */
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500; // 默认错误码
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}