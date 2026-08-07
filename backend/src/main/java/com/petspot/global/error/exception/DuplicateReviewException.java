package com.petspot.global.error.exception;

/**
 * 장소당 중복 리뷰 작성 시 발생하는 예외 (409 Conflict)
 */
public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException() {
        super("이미 이 장소에 리뷰를 작성하셨습니다.");
    }

    public DuplicateReviewException(String message) {
        super(message);
    }
}
