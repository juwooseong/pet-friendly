package com.petspot.global.error.exception;

/**
 * 리뷰 정보를 찾지 못했을 때 발생하는 예외 (404 Not Found)
 */
public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException() {
        super("리뷰 정보를 찾을 수 없습니다.");
    }

    public ReviewNotFoundException(String message) {
        super(message);
    }
}
