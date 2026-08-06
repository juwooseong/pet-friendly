package com.petspot.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.application.auth.AuthService;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.DuplicateEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register 정상 호출 시 201 Created 및 UserRegisterResponseDto 반환")
    void register_Success() throws Exception {
        // given
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .email("testuser@petspot.com")
                .password("password1234!")
                .nickname("테스트유저")
                .build();

        UserRegisterResponseDto mockResponse = UserRegisterResponseDto.builder()
                .id(UUID.randomUUID())
                .email("testuser@petspot.com")
                .nickname("테스트유저")
                .role(UserRole.USER)
                .build();

        given(authService.register(any(UserRegisterRequestDto.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("testuser@petspot.com")))
                .andExpect(jsonPath("$.data.nickname", is("테스트유저")))
                .andExpect(jsonPath("$.data.role", is("USER")));
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식 또는 짧은 비밀번호 입력 시 400 Bad Request 반환 (Validation)")
    void register_InvalidValidation_BadRequest() throws Exception {
        // given: 올바르지 않은 이메일 및 8자 미만 비밀번호
        UserRegisterRequestDto invalidRequest = UserRegisterRequestDto.builder()
                .email("invalid-email-format")
                .password("short")
                .nickname("")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 요청 시 409 Conflict 반환 (DuplicateEmailException)")
    void register_DuplicateEmail_Conflict() throws Exception {
        // given
        UserRegisterRequestDto duplicateRequest = UserRegisterRequestDto.builder()
                .email("duplicate@petspot.com")
                .password("password1234!")
                .nickname("중복유저")
                .build();

        given(authService.register(any(UserRegisterRequestDto.class)))
                .willThrow(new DuplicateEmailException("이미 사용 중인 이메일입니다."));

        // when & then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("이미 사용 중인 이메일입니다.")));
    }
}
