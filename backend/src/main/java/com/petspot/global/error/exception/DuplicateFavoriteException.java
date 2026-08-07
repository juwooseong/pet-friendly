package com.petspot.global.error.exception;

/**
 * 이미 즐겨찾기에 등록된 장소인 경우 발생하는 예외 (409 Conflict)
 */
public class DuplicateFavoriteException extends RuntimeException {

    public DuplicateFavoriteException() {
        super("이미 즐겨찾기에 등록된 장소입니다.");
    }

    public DuplicateFavoriteException(String message) {
        super(message);
    }
}
