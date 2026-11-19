package com.petspot.global.error;

import com.petspot.global.dto.ApiResponse;
import com.petspot.global.error.exception.BusinessException;
import com.petspot.global.error.exception.DuplicateEmailException;
import com.petspot.global.error.exception.DuplicateFavoriteException;
import com.petspot.global.error.exception.DuplicateNicknameException;
import com.petspot.global.error.exception.DuplicateReviewException;
import com.petspot.global.error.exception.FavoriteNotFoundException;
import com.petspot.global.error.exception.InvalidCredentialsException;
import com.petspot.global.error.exception.PetAccessDeniedException;
import com.petspot.global.error.exception.PetNotFoundException;
import com.petspot.global.error.exception.PlaceNotFoundException;
import com.petspot.global.error.exception.ReviewAccessDeniedException;
import com.petspot.global.error.exception.ReviewNotFoundException;
import com.petspot.global.error.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TSK-BE-035: 비즈니스 예외 -> ErrorCode -> HTTP 응답 매핑 검증.
 *
 * <p>예외별 처리기 12개를 {@code BusinessException} 단일 처리기로 합쳤으므로, 리팩토링 전과 같은 HTTP 상태가
 * 나가는지, 기본 메시지가 ErrorCode(= Swagger 에러 예시의 출처)와 같은지를 예외 종류별로 고정한다.
 */
class BusinessExceptionMappingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(new DuplicateEmailException(), ErrorCode.DUPLICATE_EMAIL, 409),
                Arguments.of(new DuplicateNicknameException(), ErrorCode.DUPLICATE_NICKNAME, 409),
                Arguments.of(new DuplicateFavoriteException(), ErrorCode.DUPLICATE_FAVORITE, 409),
                Arguments.of(new DuplicateReviewException(), ErrorCode.DUPLICATE_REVIEW, 409),
                Arguments.of(new InvalidCredentialsException(), ErrorCode.INVALID_CREDENTIALS, 401),
                Arguments.of(new PetAccessDeniedException(), ErrorCode.PET_ACCESS_DENIED, 403),
                Arguments.of(new ReviewAccessDeniedException(), ErrorCode.REVIEW_ACCESS_DENIED, 403),
                Arguments.of(new UserNotFoundException(), ErrorCode.USER_NOT_FOUND, 404),
                Arguments.of(new PetNotFoundException(), ErrorCode.PET_NOT_FOUND, 404),
                Arguments.of(new PlaceNotFoundException(), ErrorCode.PLACE_NOT_FOUND, 404),
                Arguments.of(new ReviewNotFoundException(), ErrorCode.REVIEW_NOT_FOUND, 404),
                Arguments.of(new FavoriteNotFoundException(), ErrorCode.FAVORITE_NOT_FOUND, 404)
        );
    }

    @ParameterizedTest(name = "{0} -> {1} ({2})")
    @MethodSource("exceptions")
    @DisplayName("각 비즈니스 예외는 정해진 ErrorCode를 가지며, 단일 처리기가 리팩토링 전과 같은 HTTP 상태와 ErrorCode 메시지로 응답한다")
    void businessException_MapsToExpectedStatusAndMessage(BusinessException ex, ErrorCode expectedCode, int expectedStatus) {
        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        // then
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isEqualTo(expectedCode.getMessage());
    }

    @Test
    @DisplayName("상황별 문구가 필요하면 메시지 생성자로 덮어쓸 수 있고, 상태 코드는 ErrorCode를 따른다 (예: 아이디 찾기)")
    void customMessage_KeepsStatusFromErrorCode() {
        // given
        UserNotFoundException ex = new UserNotFoundException("일치하는 회원 정보를 찾을 수 없습니다.");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("일치하는 회원 정보를 찾을 수 없습니다.");
    }
}
