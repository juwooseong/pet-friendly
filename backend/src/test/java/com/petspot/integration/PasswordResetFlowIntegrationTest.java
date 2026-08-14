package com.petspot.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.auth.dto.ChangePasswordRequestDto;
import com.petspot.api.auth.dto.FindIdRequestDto;
import com.petspot.api.auth.dto.FindPasswordRequestDto;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.infrastructure.email.EmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 아이디 찾기 / 비밀번호 찾기(임시 비밀번호) / 강제 비밀번호 변경 전체 흐름 E2E 통합 테스트
 * <p>
 * 흐름: 회원가입 → 아이디 찾기 → 비밀번호 찾기(임시 비밀번호 발급) → 임시 비밀번호 로그인(requiresPasswordChange=true)
 * → 강제 변경 전 일반 API 접근 차단(403) → 비밀번호 변경 → 기존 임시 비밀번호 로그인 실패
 * → 새 비밀번호 로그인 성공(requiresPasswordChange=false) → 일반 API 접근 정상 허용
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailSender emailSender;

    @Test
    @DisplayName("아이디 찾기 → 비밀번호 찾기 → 임시 비밀번호 로그인 → 강제 변경 전 접근 차단 → 비밀번호 변경 → 신규 비밀번호 로그인 전체 흐름")
    void findId_And_PasswordReset_FullFlow_Success() throws Exception {
        // ==========================================
        // 0. 회원가입
        // ==========================================
        String email = "reset_flow@petspot.com";
        String originalPassword = "OriginalPass1!";
        String nickname = "비번찾기유저";

        UserRegisterRequestDto registerDto = UserRegisterRequestDto.builder()
                .email(email)
                .password(originalPassword)
                .nickname(nickname)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());

        // ==========================================
        // 1. 아이디(이메일) 찾기 (POST /api/v1/auth/find-id)
        // ==========================================
        FindIdRequestDto findIdDto = FindIdRequestDto.builder()
                .nickname(nickname)
                .build();

        mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(findIdDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.maskedEmail", is("r*********@petspot.com")));

        // ==========================================
        // 2. 비밀번호 찾기 (POST /api/v1/auth/find-password) → 임시 비밀번호 이메일 발송
        // ==========================================
        doNothing().when(emailSender).sendTemporaryPassword(eq(email), org.mockito.ArgumentMatchers.anyString());

        FindPasswordRequestDto findPasswordDto = FindPasswordRequestDto.builder()
                .email(email)
                .build();

        mockMvc.perform(post("/api/v1/auth/find-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(findPasswordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 이메일 발송 호출에서 평문 임시 비밀번호 캡처 (API 응답에는 노출되지 않음)
        ArgumentCaptor<String> temporaryPasswordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendTemporaryPassword(eq(email), temporaryPasswordCaptor.capture());
        String temporaryPassword = temporaryPasswordCaptor.getValue();
        assertThat(temporaryPassword).isNotBlank();

        // ==========================================
        // 3. 임시 비밀번호로 로그인 → requiresPasswordChange=true 및 JWT 발급 확인
        // ==========================================
        UserLoginRequestDto tempLoginDto = UserLoginRequestDto.builder()
                .email(email)
                .password(temporaryPassword)
                .build();

        MvcResult tempLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tempLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.data.requiresPasswordChange", is(true)))
                .andReturn();

        String tempAccessToken = extractAccessToken(tempLoginResult);
        String tempAuthHeader = "Bearer " + tempAccessToken;

        // ==========================================
        // 4. 강제 비밀번호 변경 완료 전에는 일반 API(예: GET /api/v1/users/me) 접근이 차단된다 (403)
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", tempAuthHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(header().string("X-Password-Change-Required", "true"));

        // ==========================================
        // 5. 비밀번호 변경 (PATCH /api/v1/auth/password) - 강제 변경 상태에서도 허용되어야 함
        // ==========================================
        String newPassword = "BrandNewPass2@";
        ChangePasswordRequestDto changePasswordDto = ChangePasswordRequestDto.builder()
                .newPassword(newPassword)
                .confirmPassword(newPassword)
                .build();

        mockMvc.perform(patch("/api/v1/auth/password")
                        .header("Authorization", tempAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // ==========================================
        // 6. 기존 임시 비밀번호로는 더 이상 로그인할 수 없다 (401)
        // ==========================================
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tempLoginDto)))
                .andExpect(status().isUnauthorized());

        // ==========================================
        // 7. 새로운 비밀번호로 로그인 성공 → requiresPasswordChange=false
        // ==========================================
        UserLoginRequestDto newLoginDto = UserLoginRequestDto.builder()
                .email(email)
                .password(newPassword)
                .build();

        MvcResult newLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange", is(false)))
                .andReturn();

        String newAccessToken = extractAccessToken(newLoginResult);

        // ==========================================
        // 8. 비밀번호 변경 완료 후에는 일반 API 접근이 정상적으로 허용된다
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email)));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 찾기 요청 시에도 200 OK, 이메일 발송은 호출되지 않는다")
    void findPassword_NonExistentEmail_NoEmailSent() throws Exception {
        FindPasswordRequestDto request = FindPasswordRequestDto.builder()
                .email("no_such_user@petspot.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/find-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        org.mockito.Mockito.verifyNoInteractions(emailSender);
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        String responseJson = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        return jsonNode.get("data").get("accessToken").asText();
    }
}
