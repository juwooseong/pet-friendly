package com.petspot.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.auth.dto.ChangePasswordRequestDto;
import com.petspot.api.auth.dto.FindIdRequestDto;
import com.petspot.api.auth.dto.FindIdResponseDto;
import com.petspot.api.auth.dto.FindPasswordRequestDto;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.application.auth.AuthService;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.DuplicateEmailException;
import com.petspot.global.error.exception.InvalidCredentialsException;
import com.petspot.global.error.exception.UserNotFoundException;
import com.petspot.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private UsernamePasswordAuthenticationToken mockAuth;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(401);
            response.getWriter().write("{\"success\":false,\"error\":\"인증이 필요한 요청입니다.\"}");
            return null;
        }).when(customAuthenticationEntryPoint).commence(any(), any(), any());

        User mockUser = User.register("me@petspot.com", "hash", "내닉네임");
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

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

    @Test
    @DisplayName("로그인 응답에 requiresPasswordChange 필드가 포함되어 반환된다")
    void login_Success_IncludesRequiresPasswordChange() throws Exception {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("temp@petspot.com")
                .password("TempPassw0rd!")
                .build();

        UserLoginResponseDto mockResponse = UserLoginResponseDto.builder()
                .accessToken("mock.jwt.token.string")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .requiresPasswordChange(true)
                .build();

        given(authService.login(any(UserLoginRequestDto.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/find-id 정상 호출 시 200 OK 및 마스킹된 이메일 반환")
    void findId_Success() throws Exception {
        // given
        FindIdRequestDto request = FindIdRequestDto.builder()
                .nickname("뽀삐아빠")
                .build();

        FindIdResponseDto mockResponse = FindIdResponseDto.builder()
                .maskedEmail("j*********@gmail.com")
                .build();

        given(authService.findId(any(FindIdRequestDto.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.maskedEmail", is("j*********@gmail.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/find-id 일치하는 닉네임이 없으면 404 Not Found 반환")
    void findId_NotFound() throws Exception {
        // given
        FindIdRequestDto request = FindIdRequestDto.builder()
                .nickname("존재하지않는닉네임")
                .build();

        given(authService.findId(any(FindIdRequestDto.class)))
                .willThrow(new UserNotFoundException("일치하는 회원 정보를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/find-id 닉네임 누락 시 400 Bad Request 반환")
    void findId_MissingNickname_BadRequest() throws Exception {
        // given
        FindIdRequestDto invalidRequest = FindIdRequestDto.builder().nickname("").build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/find-password 정상 호출 시 200 OK 및 임시 비밀번호 미노출 응답")
    void findPassword_Success() throws Exception {
        // given
        FindPasswordRequestDto request = FindPasswordRequestDto.builder()
                .email("active@petspot.com")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/find-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/find-password 존재하지 않는 이메일에도 동일하게 200 OK 반환 (계정 존재 여부 노출 방지)")
    void findPassword_NonExistentEmail_StillReturnsOk() throws Exception {
        // given
        FindPasswordRequestDto request = FindPasswordRequestDto.builder()
                .email("none@petspot.com")
                .build();

        // authService.findPassword()는 존재 여부와 무관하게 정상 반환(void)되도록 구현되어 있으므로
        // 별도 stubbing 없이도 mock은 아무 예외를 던지지 않는다.

        // when & then
        mockMvc.perform(post("/api/v1/auth/find-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/password 인증된 사용자가 정상 요청 시 200 OK 반환")
    void changePassword_Success() throws Exception {
        // given
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .newPassword("NewPassw0rd!")
                .confirmPassword("NewPassw0rd!")
                .build();

        // when & then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/password 인증 헤더 없이 요청 시 401 Unauthorized 반환")
    void changePassword_Unauthenticated_Unauthorized() throws Exception {
        // given
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .newPassword("NewPassw0rd!")
                .confirmPassword("NewPassw0rd!")
                .build();

        // when & then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/password 비밀번호 정책(영문/숫자/특수문자 조합) 위반 시 400 Bad Request 반환")
    void changePassword_PolicyViolation_BadRequest() throws Exception {
        // given: 특수문자와 숫자가 없는 비밀번호
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .newPassword("onlyletters")
                .confirmPassword("onlyletters")
                .build();

        // when & then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/password 새 비밀번호와 확인 비밀번호 불일치 시 400 Bad Request 반환")
    void changePassword_Mismatch_BadRequest() throws Exception {
        // given
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .newPassword("NewPassw0rd!")
                .confirmPassword("Different1!")
                .build();

        org.mockito.BDDMockito.willThrow(new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다."))
                .given(authService).changePassword(any(UUID.class), any(ChangePasswordRequestDto.class));

        // when & then
        mockMvc.perform(patch("/api/v1/auth/password")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.")));
    }
}
