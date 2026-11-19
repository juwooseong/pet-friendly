package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;

/**
 * 다른 회원의 반려동물 접근 (403 Forbidden)
 *
 * <p>HTTP 상태와 기본 메시지는 {@link ErrorCode#PET_ACCESS_DENIED}에서 가져온다(TSK-BE-035).
 * 기본 생성자를 쓰면 ErrorCode 메시지가 그대로 응답되며, 특정 상황에서만 다른 문구가 필요할 때 메시지 생성자를 쓴다.
 */
public class PetAccessDeniedException extends BusinessException {

    public PetAccessDeniedException() {
        super(ErrorCode.PET_ACCESS_DENIED);
    }

    public PetAccessDeniedException(String message) {
        super(ErrorCode.PET_ACCESS_DENIED, message);
    }
}
