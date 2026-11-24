package com.petspot.global.error.exception;

import com.petspot.global.error.ErrorCode;

/**
 * 리뷰 사진 업로드·조회 오류 (형식 오류 400, 크기 초과 413, 없음 404).
 */
public class ReviewImageException extends BusinessException {

    public ReviewImageException(ErrorCode errorCode) {
        super(errorCode);
    }
}
