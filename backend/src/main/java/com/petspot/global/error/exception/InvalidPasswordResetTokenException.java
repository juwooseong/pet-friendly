package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;

/**
 * 비밀번호 재설정 링크가 없거나, 만료되었거나, 이미 사용된 경우 (400, TSK-SEC-003).
 * 어떤 이유인지는 구분해 알려 주지 않는다.
 */
public class InvalidPasswordResetTokenException extends BusinessException {

    public InvalidPasswordResetTokenException() {
        super(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
    }
}
