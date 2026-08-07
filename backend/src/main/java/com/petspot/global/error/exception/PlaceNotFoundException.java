package com.petspot.global.error.exception;

/**
 * 장소 정보를 찾지 못했을 때 발생하는 예외 (404 Not Found)
 */
public class PlaceNotFoundException extends RuntimeException {

    public PlaceNotFoundException() {
        super("장소 정보를 찾을 수 없습니다.");
    }

    public PlaceNotFoundException(String message) {
        super(message);
    }
}
