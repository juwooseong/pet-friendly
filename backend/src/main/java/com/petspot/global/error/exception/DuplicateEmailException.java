package com.petspot.global.error.exception;

/**
 * 이메일 중복 시 발생하는 예외 (409 Conflict)
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
