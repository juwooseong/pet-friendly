package com.petspot.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TSK-BE-023: 클라이언트 입력 형식 오류가 500이 아닌 400으로 응답되는지 검증한다.
 *
 * <p>이전에는 {@code MethodArgumentTypeMismatchException}(잘못된 UUID 경로 변수)과
 * {@code HttpMessageNotReadableException}(깨진 JSON 본문)에 대한 처리기가 없어 최상위 {@code Exception} 처리기로
 * 떨어져 "서버 내부 오류"(500)로 응답했다. Swagger 문서에 400(COMMON_400)으로 명세한 내용과 실제 동작을 맞춘다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvalidRequestFormatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("UUID 형식이 아닌 경로 변수로 호출하면 400과 파라미터 형식 오류 메시지를 반환한다")
    void invalidUuidPathVariable_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/places/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("요청 파라미터 'placeId'의 형식이 올바르지 않습니다.")));
    }

    @Test
    @DisplayName("JSON 문법이 깨진 요청 본문은 400과 고정 메시지를 반환하고 내부 파서 메시지를 노출하지 않는다")
    void malformedJsonBody_Returns400WithoutParserDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"user@petspot.com\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("요청 본문의 형식이 올바르지 않습니다.")))
                .andExpect(jsonPath("$.error", not(containsString("JSON parse error"))));
    }
}
