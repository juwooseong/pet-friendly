package com.petspot.global.error;

import com.petspot.global.dto.ApiResponse;
import com.petspot.global.error.exception.BusinessException;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 전역 예외 처리기 (Global Exception Handler)
 * 클라이언트 4xx 에러는 WARN 로깅, 서버 5xx 에러는 ERROR 로깅 및 보안 메시지 마스킹 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_SERVER_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[CLIENT ERROR] IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(org.springframework.validation.BindException ex) {
        String errorMsg = ex.getBindingResult().getAllErrors().isEmpty() ? "유효하지 않은 요청입니다." : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[CLIENT ERROR] BindException: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMsg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getAllErrors().isEmpty() ? "유효하지 않은 요청입니다." : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[CLIENT ERROR] MethodArgumentNotValidException: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMsg));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        String errorMsg = "유효하지 않은 요청 파라미터입니다.";
        if (!ex.getAllErrors().isEmpty() && ex.getAllErrors().get(0).getDefaultMessage() != null) {
            errorMsg = ex.getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("[CLIENT ERROR] HandlerMethodValidationException: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMsg));
    }

    /**
     * {@code @Validated} 클래스 + {@code @RequestParam} 제약조건(예: check-email/check-nickname) 검증 실패 시 발생.
     * spring-boot-starter-validation의 AOP 기반 메서드 검증(MethodValidationPostProcessor)이
     * Spring 6.1의 신규 HandlerMethodValidationException보다 우선 적용되어 이 예외가 실제로 던져진다.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        String errorMsg = ex.getConstraintViolations().isEmpty()
                ? "유효하지 않은 요청 파라미터입니다."
                : ex.getConstraintViolations().iterator().next().getMessage();
        log.warn("[CLIENT ERROR] ConstraintViolationException: {}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMsg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("[CLIENT ERROR] MissingServletRequestParameterException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 경로/쿼리 파라미터 타입 변환 실패 (예: {@code /api/v1/places/not-a-uuid}).
     * TSK-BE-023 이전에는 처리기가 없어 최상위 {@code Exception} 처리기로 떨어져 500을 반환했다 - 클라이언트 입력 오류이므로 400으로 응답한다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String errorMsg = "요청 파라미터 '" + ex.getName() + "'의 형식이 올바르지 않습니다.";
        log.warn("[CLIENT ERROR] MethodArgumentTypeMismatchException: {} (value={})", errorMsg, ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMsg));
    }

    /**
     * 요청 본문(JSON) 파싱 실패 (문법 오류, enum/날짜/UUID 형식 오류, 본문 누락 등).
     * TSK-BE-023 이전에는 500을 반환했다 - 400으로 응답하며, 내부 파서 메시지는 노출하지 않는다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[CLIENT ERROR] HttpMessageNotReadableException: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("요청 본문의 형식이 올바르지 않습니다."));
    }

    /** 업로드 파일이 spring.servlet.multipart 한도를 넘은 경우 (리뷰 사진) */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("[CLIENT ERROR] MaxUploadSizeExceededException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.REVIEW_IMAGE_TOO_LARGE;
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode.getMessage()));
    }

    @ExceptionHandler(PublicDataException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicDataException(PublicDataException ex) {
        log.warn("[EXTERNAL API ERROR] PublicDataException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 모든 비즈니스 예외(중복/없음/권한 등) 공통 처리 (TSK-BE-035).
     * HTTP 상태는 예외가 가진 {@link ErrorCode}에서 결정한다 - 예외 종류가 늘어나도 처리기를 추가할 필요가 없다.
     * 이전에는 예외 클래스마다 처리기를 하나씩(12개) 두고 상태 코드를 손으로 적었다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[CLIENT ERROR] {} ({}): {}", ex.getClass().getSimpleName(), errorCode.getCode(), ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("[SERVER INTERNAL ERROR] Unhandled Exception occurred: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(DEFAULT_SERVER_ERROR_MESSAGE));
    }
}
