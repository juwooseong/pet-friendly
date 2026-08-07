package com.petspot.global.error.exception;

/**
 * 반려동물 정보를 찾지 못했을 때 발생하는 예외 (404 Not Found)
 */
public class PetNotFoundException extends RuntimeException {

    public PetNotFoundException() {
        super("반려동물 정보를 찾을 수 없습니다.");
    }

    public PetNotFoundException(String message) {
        super(message);
    }
}
