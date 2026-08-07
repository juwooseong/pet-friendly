package com.petspot.global.error.exception;

/**
 * 다른 사용자의 반려동물 정보에 접근을 시도할 때 발생하는 예외 (403 Forbidden)
 */
public class PetAccessDeniedException extends RuntimeException {

    public PetAccessDeniedException() {
        super("해당 반려동물 정보에 접근할 권한이 없습니다.");
    }

    public PetAccessDeniedException(String message) {
        super(message);
    }
}
