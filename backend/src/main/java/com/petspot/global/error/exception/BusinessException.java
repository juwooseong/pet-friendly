package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;
import lombok.Getter;

/**
 * 모든 비즈니스 예외의 최상위 클래스
 * ErrorCode 를 보유하여 GlobalExceptionHandler 가 오류 코드 및 HTTP 상태를 일괄 처리한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
