package com.example.k5_iot_springboot.common.errors;

import com.example.k5_iot_springboot.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String reason;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.reason = null;
    }

    public BusinessException(ErrorCode errorCode, String reason) {
        super(errorCode.name() + (reason != null ? (": " + reason) : ""));
        this.errorCode = errorCode;
        this.reason = reason;
    }
}