package com.petspot.global.swagger;

import com.petspot.global.error.ErrorCode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드가 반환할 수 있는 <b>비즈니스 에러</b>를 Swagger(OpenAPI) 문서에 선언한다 (TSK-BE-023).
 *
 * <p>여기에는 해당 API의 서비스 로직이 실제로 던지는 예외만 적는다(예: 404 장소 없음, 409 중복 리뷰).
 * 모든 API에 공통으로 적용되는 에러는 {@link ApiErrorResponseCustomizer}가 자동으로 붙이므로 적지 않는다:
 * <ul>
 *   <li>400 {@link ErrorCode#INVALID_INPUT_VALUE} - 요청 본문/파라미터가 있는 API</li>
 *   <li>401 {@link ErrorCode#UNAUTHENTICATED} - 인증이 필요한 API</li>
 *   <li>403 {@link ErrorCode#PASSWORD_CHANGE_REQUIRED} - 인증이 필요하고 비밀번호 강제 변경 인터셉터 대상인 API</li>
 *   <li>500 {@link ErrorCode#INTERNAL_SERVER_ERROR} - 전체 API</li>
 * </ul>
 *
 * <pre>{@code
 * @ApiErrorCodes({ErrorCode.PLACE_NOT_FOUND, ErrorCode.DUPLICATE_REVIEW})
 * @PostMapping("/api/v1/reviews")
 * public ResponseEntity<...> createReview(...) { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

    /** 이 API가 반환할 수 있는 비즈니스 에러 코드 목록 */
    ErrorCode[] value();
}
