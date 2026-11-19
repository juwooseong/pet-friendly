package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;

/**
 * 이미 즐겨찾기에 등록된 장소 (409 Conflict)
 *
 * <p>HTTP 상태와 기본 메시지는 {@link ErrorCode#DUPLICATE_FAVORITE}에서 가져온다(TSK-BE-035).
 * 기본 생성자를 쓰면 ErrorCode 메시지가 그대로 응답되며, 특정 상황에서만 다른 문구가 필요할 때 메시지 생성자를 쓴다.
 */
public class DuplicateFavoriteException extends BusinessException {

    public DuplicateFavoriteException() {
        super(ErrorCode.DUPLICATE_FAVORITE);
    }

    public DuplicateFavoriteException(String message) {
        super(ErrorCode.DUPLICATE_FAVORITE, message);
    }
}
