package com.petspot.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.application.auth.AuthService;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.DuplicateEmailException;
import com.petspot.global.error.exception.InvalidCredentialsException;
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
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.petspot.global.util.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.petspot.global.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.petspot.global.security.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

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

    @Test
    @DisplayName("POST /api/v1/auth/login 정상 호출 시 200 OK 및 JWT Access Token 반환")
    void login_Success() throws Exception {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("testuser@petspot.com")
                .password("password1234!")
                .build();

        UserLoginResponseDto mockResponse = UserLoginResponseDto.builder()
                .accessToken("mock.jwt.token.string")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build();

        given(authService.login(any(UserLoginRequestDto.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", is("mock.jwt.token.string")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", is(86400)));
    }

    @Test
    @DisplayName("로그인 요청 시 유효하지 않은 이메일 형식 또는 빈 비밀번호인 경우 400 Bad Request 반환")
    void login_InvalidValidation_BadRequest() throws Exception {
        // given
        UserLoginRequestDto invalidRequest = UserLoginRequestDto.builder()
                .email("invalid-email")
                .password("")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("로그인 실패 시 (잘못된 이메일/비밀번호 또는 비활성 상태) 401 Unauthorized 반환")
    void login_InvalidCredentials_Unauthorized() throws Exception {
        // given
        UserLoginRequestDto failRequest = UserLoginRequestDto.builder()
                .email("fail@petspot.com")
                .password("wrongpassword")
                .build();

        given(authService.login(any(UserLoginRequestDto.class)))
                .willThrow(new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("이메일 또는 비밀번호가 올바르지 않습니다.")));
    }
}
