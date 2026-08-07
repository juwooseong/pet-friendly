package com.petspot.global.error.exception;

/**
 * 즐겨찾기 항목을 찾지 못했을 때 발생하는 예외 (404 Not Found)
 */
public class FavoriteNotFoundException extends RuntimeException {

    public FavoriteNotFoundException() {
        super("즐겨찾기 항목을 찾을 수 없습니다.");
    }

    public FavoriteNotFoundException(String message) {
        super(message);
    }
}
