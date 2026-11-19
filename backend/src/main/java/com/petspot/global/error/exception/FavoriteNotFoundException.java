package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;

/**
 * 즐겨찾기 항목 없음 (404 Not Found)
 *
 * <p>HTTP 상태와 기본 메시지는 {@link ErrorCode#FAVORITE_NOT_FOUND}에서 가져온다(TSK-BE-035).
 * 기본 생성자를 쓰면 ErrorCode 메시지가 그대로 응답되며, 특정 상황에서만 다른 문구가 필요할 때 메시지 생성자를 쓴다.
 */
public class FavoriteNotFoundException extends BusinessException {

    public FavoriteNotFoundException() {
        super(ErrorCode.FAVORITE_NOT_FOUND);
    }

    public FavoriteNotFoundException(String message) {
        super(ErrorCode.FAVORITE_NOT_FOUND, message);
    }
}
