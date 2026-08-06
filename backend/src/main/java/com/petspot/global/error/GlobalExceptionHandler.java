package com.petspot.global.error;

import com.petspot.global.dto.ApiResponse;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("[CLIENT ERROR] MissingServletRequestParameterException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PublicDataException.class)
    public ResponseEntity<ApiResponse<Void>> handlePublicDataException(PublicDataException ex) {
        log.warn("[EXTERNAL API ERROR] PublicDataException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(com.petspot.global.error.exception.DuplicateEmailException ex) {
        log.warn("[CLIENT ERROR] DuplicateEmailException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("[SERVER INTERNAL ERROR] Unhandled Exception occurred: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(DEFAULT_SERVER_ERROR_MESSAGE));
    }
}
