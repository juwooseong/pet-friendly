package com.petspot.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 오류 코드 정의
 * 각 오류 코드는 HTTP 상태 코드와 기본 응답 메시지를 함께 보유한다.
 */
@Getter
public enum ErrorCode {

    // ===== 공통 (COMMON) =====
    INVALID_INPUT_VALUE("COMMON_400", HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    INTERNAL_SERVER_ERROR("COMMON_500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // ===== 인증/인가 (AUTH) =====
    DUPLICATE_EMAIL("AUTH_001", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS("AUTH_002", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHENTICATED("AUTH_003", HttpStatus.UNAUTHORIZED, "인증이 필요한 요청입니다."),

    // ===== 회원 (USER) =====
    USER_NOT_FOUND("USER_001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ===== 반려동물 (PET) =====
    PET_NOT_FOUND("PET_001", HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),
    PET_ACCESS_DENIED("PET_002", HttpStatus.FORBIDDEN, "해당 반려동물 정보에 접근할 권한이 없습니다."),

    // ===== 장소 (PLACE) =====
    PLACE_NOT_FOUND("PLACE_001", HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),

    // ===== 리뷰 (REVIEW) =====
    REVIEW_NOT_FOUND("REVIEW_001", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    DUPLICATE_REVIEW("REVIEW_002", HttpStatus.CONFLICT, "이미 이 장소에 리뷰를 작성하셨습니다."),
    REVIEW_ACCESS_DENIED("REVIEW_003", HttpStatus.FORBIDDEN, "해당 리뷰를 수정 또는 삭제할 권한이 없습니다."),

    // ===== 즐겨찾기 (FAVORITE) =====
    FAVORITE_NOT_FOUND("FAVORITE_001", HttpStatus.NOT_FOUND, "즐겨찾기 항목을 찾을 수 없습니다."),
    DUPLICATE_FAVORITE("FAVORITE_002", HttpStatus.CONFLICT, "이미 즐겨찾기에 등록된 장소입니다."),

    // ===== 외부 연동 (EXTERNAL) =====
    EXTERNAL_API_ERROR("EXTERNAL_001", HttpStatus.BAD_GATEWAY, "외부 API 연동 중 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
