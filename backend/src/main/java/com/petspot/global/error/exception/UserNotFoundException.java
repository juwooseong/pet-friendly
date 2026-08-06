package com.petspot.global.error.exception;

/**
 * 존재하지 않는 사용자 조회 시 발생하는 예외 (404 Not Found)
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("사용자를 찾을 수 없습니다.");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
