package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;

/**
 * 반려동물 없음 (404 Not Found)
 *
 * <p>HTTP 상태와 기본 메시지는 {@link ErrorCode#PET_NOT_FOUND}에서 가져온다(TSK-BE-035).
 * 기본 생성자를 쓰면 ErrorCode 메시지가 그대로 응답되며, 특정 상황에서만 다른 문구가 필요할 때 메시지 생성자를 쓴다.
 */
public class PetNotFoundException extends BusinessException {

    public PetNotFoundException() {
        super(ErrorCode.PET_NOT_FOUND);
    }

    public PetNotFoundException(String message) {
        super(ErrorCode.PET_NOT_FOUND, message);
    }
}
