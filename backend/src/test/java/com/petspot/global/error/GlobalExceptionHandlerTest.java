package com.petspot.global.error;

import com.petspot.global.dto.ApiResponse;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("서버 예외(500) 발생 시 보안을 위해 마스킹된 고정 메시지를 반환하고 internal message를 노출하지 않는다")
    void handleGeneric_MasksInternalExceptionMessage() {
        // given
        Exception internalException = new RuntimeException("SQLSyntaxErrorException: Table 'places' does not exist at secret_db_host:5432");

        // when
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeneric(internalException);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(response.getBody().getError()).doesNotContain("SQLSyntaxErrorException");
        assertThat(response.getBody().getError()).doesNotContain("secret_db_host");
    }

    @Test
    @DisplayName("클라이언트 IllegalArgumentException(400) 발생 시 400 Bad Request와 메시지를 반환한다")
    void handleIllegalArgument_ReturnsBadRequest() {
        // given
        IllegalArgumentException ex = new IllegalArgumentException("유효하지 않은 요청 인자입니다.");

        // when
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgument(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isEqualTo("유효하지 않은 요청 인자입니다.");
    }

    @Test
    @DisplayName("외부 API PublicDataException(502) 발생 시 502 Bad Gateway를 반환한다")
    void handlePublicDataException_ReturnsBadGateway() {
        // given
        PublicDataException ex = new PublicDataException("TourAPI 통신 실패");

        // when
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handlePublicDataException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isEqualTo("TourAPI 통신 실패");
    }
}
