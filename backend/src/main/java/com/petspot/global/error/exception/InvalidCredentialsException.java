package com.petspot.global.error.exception;

/**
 * 로그인 인증 실패 시 발생하는 예외 (401 Unauthorized)
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
