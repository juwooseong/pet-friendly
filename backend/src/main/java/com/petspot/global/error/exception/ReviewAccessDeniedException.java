package com.petspot.global.error.exception;

/**
 * 타인의 리뷰 수정/삭제 시 발생하는 접근 권한 예외 (403 Forbidden)
 */
public class ReviewAccessDeniedException extends RuntimeException {

    public ReviewAccessDeniedException() {
        super("해당 리뷰를 수정 또는 삭제할 권한이 없습니다.");
    }

    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}
