package com.petspot.application.auth;

import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateEmailException;
import com.petspot.global.error.exception.InvalidCredentialsException;
import com.petspot.global.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("정상 회원가입 요청 시 이메일 중복 체크, 비밀번호 암호화 및 User 저장 성공")
    void register_Success() {
        // given
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .email("newuser@petspot.com")
                .password("password1234!")
                .nickname("신규유저")
                .build();

        given(userRepository.existsByEmail("newuser@petspot.com")).willReturn(false);
        given(passwordEncoder.encode("password1234!")).willReturn("bcrypt_encoded_hash");

        User mockSavedUser = User.register("newuser@petspot.com", "bcrypt_encoded_hash", "신규유저");

        given(userRepository.save(any(User.class))).willReturn(mockSavedUser);

        // when
        UserRegisterResponseDto response = authService.register(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("newuser@petspot.com");
        assertThat(response.getNickname()).isEqualTo("신규유저");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);

        verify(userRepository).existsByEmail("newuser@petspot.com");
        verify(passwordEncoder).encode("password1234!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미 존재(중복)하는 이메일로 회원가입 시 DuplicateEmailException 예외 발생")
    void register_DuplicateEmail_ThrowsException() {
        // given
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .email("existing@petspot.com")
                .password("password1234!")
                .nickname("기존유저")
                .build();

        given(userRepository.existsByEmail("existing@petspot.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        verify(userRepository).existsByEmail("existing@petspot.com");
    }

    @Test
    @DisplayName("정상 이메일 및 비밀번호 로그인 시 JWT Access Token 발급 성공")
    void login_Success() {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("active@petspot.com")
                .password("password1234!")
                .build();

        User activeUser = User.register("active@petspot.com", "bcrypt_pwd_hash", "활성유저");

        given(userRepository.findByEmail("active@petspot.com")).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches("password1234!", "bcrypt_pwd_hash")).willReturn(true);
        given(jwtTokenProvider.generateToken(any(), any(), any())).willReturn("mock.jwt.access.token");
        given(jwtTokenProvider.getExpirationMs()).willReturn(86400000L);

        // when
        UserLoginResponseDto response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.access.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("존재하지 않는 회원 이메일로 로그인 시 InvalidCredentialsException 발생")
    void login_NonExistentUser_ThrowsException() {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("none@petspot.com")
                .password("password1234!")
                .build();

        given(userRepository.findByEmail("none@petspot.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 InvalidCredentialsException 발생")
    void login_PasswordMismatch_ThrowsException() {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("active@petspot.com")
                .password("wrongpassword")
                .build();

        User activeUser = User.register("active@petspot.com", "bcrypt_pwd_hash", "활성유저");

        given(userRepository.findByEmail("active@petspot.com")).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches("wrongpassword", "bcrypt_pwd_hash")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("탈퇴한 회원(WITHDRAWN) 로그인 시 InvalidCredentialsException 발생")
    void login_WithdrawnUser_ThrowsException() {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("withdrawn@petspot.com")
                .password("password1234!")
                .build();

        User withdrawnUser = User.register("withdrawn@petspot.com", "bcrypt_pwd_hash", "탈퇴유저");
        withdrawnUser.withdraw();

        given(userRepository.findByEmail("withdrawn@petspot.com")).willReturn(Optional.of(withdrawnUser));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("비활성 회원(INACTIVE) 로그인 시 InvalidCredentialsException 발생")
    void login_InactiveUser_ThrowsException() {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("inactive@petspot.com")
                .password("password1234!")
                .build();

        User inactiveUser = User.register("inactive@petspot.com", "bcrypt_pwd_hash", "비활성유저");
        inactiveUser.changeStatus(UserStatus.INACTIVE);

        given(userRepository.findByEmail("inactive@petspot.com")).willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
