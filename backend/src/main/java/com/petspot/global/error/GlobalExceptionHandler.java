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

    @ExceptionHandler(com.petspot.global.error.exception.DuplicateNicknameException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateNickname(com.petspot.global.error.exception.DuplicateNicknameException ex) {
        log.warn("[CLIENT ERROR] DuplicateNicknameException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(com.petspot.global.error.exception.InvalidCredentialsException ex) {
        log.warn("[AUTH FAILED] InvalidCredentialsException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(com.petspot.global.error.exception.UserNotFoundException ex) {
        log.warn("[CLIENT ERROR] UserNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.PetNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePetNotFound(com.petspot.global.error.exception.PetNotFoundException ex) {
        log.warn("[CLIENT ERROR] PetNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.PetAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePetAccessDenied(com.petspot.global.error.exception.PetAccessDeniedException ex) {
        log.warn("[ACCESS DENIED] PetAccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.PlaceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlaceNotFound(com.petspot.global.error.exception.PlaceNotFoundException ex) {
        log.warn("[CLIENT ERROR] PlaceNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.ReviewNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewNotFound(com.petspot.global.error.exception.ReviewNotFoundException ex) {
        log.warn("[CLIENT ERROR] ReviewNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.DuplicateReviewException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateReview(com.petspot.global.error.exception.DuplicateReviewException ex) {
        log.warn("[CLIENT ERROR] DuplicateReviewException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.ReviewAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewAccessDenied(com.petspot.global.error.exception.ReviewAccessDeniedException ex) {
        log.warn("[ACCESS DENIED] ReviewAccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.FavoriteNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleFavoriteNotFound(com.petspot.global.error.exception.FavoriteNotFoundException ex) {
        log.warn("[CLIENT ERROR] FavoriteNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(com.petspot.global.error.exception.DuplicateFavoriteException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateFavorite(com.petspot.global.error.exception.DuplicateFavoriteException ex) {
        log.warn("[CLIENT ERROR] DuplicateFavoriteException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("[SERVER INTERNAL ERROR] Unhandled Exception occurred: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(DEFAULT_SERVER_ERROR_MESSAGE));
    }
}
