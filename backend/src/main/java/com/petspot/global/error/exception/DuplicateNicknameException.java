package com.petspot.global.error.exception;

/**
 * 닉네임 중복 시 발생하는 예외 (409 Conflict)
 */
public class DuplicateNicknameException extends RuntimeException {

    public DuplicateNicknameException(String message) {
        super(message);
    }
}
