package com.petspot.application.auth;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.petspot.api.auth.dto.AvailabilityResponseDto;
import com.petspot.api.auth.dto.ChangePasswordRequestDto;
import com.petspot.api.auth.dto.FindIdRequestDto;
import com.petspot.api.auth.dto.FindIdResponseDto;
import com.petspot.api.auth.dto.FindPasswordRequestDto;
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
import com.petspot.global.error.exception.UserNotFoundException;
import com.petspot.global.util.JwtTokenProvider;
import com.petspot.global.util.TemporaryPasswordGenerator;
import com.petspot.infrastructure.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Mock
    private EmailSender emailSender;

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
    @DisplayName("이미 존재(중복)하는 닉네임으로 회원가입 시 DuplicateNicknameException 예외 발생")
    void register_DuplicateNickname_ThrowsException() {
        // given
        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .email("newuser2@petspot.com")
                .password("password1234!")
                .nickname("중복닉네임")
                .build();

        given(userRepository.existsByEmail("newuser2@petspot.com")).willReturn(false);
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(com.petspot.global.error.exception.DuplicateNicknameException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");

        verify(userRepository).existsByNickname("중복닉네임");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("존재하는 이메일로 가용성 확인 시 available=false 반환")
    void checkEmailAvailability_ExistingEmail_ReturnsFalse() {
        // given
        given(userRepository.existsByEmail("existing@petspot.com")).willReturn(true);

        // when
        AvailabilityResponseDto response = authService.checkEmailAvailability("existing@petspot.com");

        // then
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 가용성 확인 시 available=true 반환")
    void checkEmailAvailability_NewEmail_ReturnsTrue() {
        // given
        given(userRepository.existsByEmail("new@petspot.com")).willReturn(false);

        // when
        AvailabilityResponseDto response = authService.checkEmailAvailability("new@petspot.com");

        // then
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("존재하는 닉네임으로 가용성 확인 시 available=false 반환")
    void checkNicknameAvailability_ExistingNickname_ReturnsFalse() {
        // given
        given(userRepository.existsByNickname("기존닉네임")).willReturn(true);

        // when
        AvailabilityResponseDto response = authService.checkNicknameAvailability("기존닉네임");

        // then
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 닉네임으로 가용성 확인 시 available=true 반환")
    void checkNicknameAvailability_NewNickname_ReturnsTrue() {
        // given
        given(userRepository.existsByNickname("신규닉네임")).willReturn(false);

        // when
        AvailabilityResponseDto response = authService.checkNicknameAvailability("신규닉네임");

        // then
        assertThat(response.isAvailable()).isTrue();
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

    @Test
    @DisplayName("임시 비밀번호로 로그인한 사용자는 requiresPasswordChange=true 로 응답한다")
    void login_TemporaryPasswordUser_RequiresPasswordChangeTrue() {
        // given
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .email("temp@petspot.com")
                .password("TempPassw0rd!")
                .build();

        User tempPasswordUser = User.register("temp@petspot.com", "bcrypt_temp_hash", "임시유저");
        tempPasswordUser.issueTemporaryPassword("bcrypt_temp_hash");

        given(userRepository.findByEmail("temp@petspot.com")).willReturn(Optional.of(tempPasswordUser));
        given(passwordEncoder.matches("TempPassw0rd!", "bcrypt_temp_hash")).willReturn(true);
        given(jwtTokenProvider.generateToken(any(), any(), any())).willReturn("mock.jwt.access.token");
        given(jwtTokenProvider.getExpirationMs()).willReturn(86400000L);

        // when
        UserLoginResponseDto response = authService.login(request);

        // then
        assertThat(response.isRequiresPasswordChange()).isTrue();
    }

    @Test
    @DisplayName("일반 비밀번호로 로그인한 사용자는 requiresPasswordChange=false 로 응답한다")
    void login_NormalUser_RequiresPasswordChangeFalse() {
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
        assertThat(response.isRequiresPasswordChange()).isFalse();
    }

    @Test
    @DisplayName("가입된 닉네임으로 아이디 찾기 요청 시 마스킹된 이메일을 반환한다")
    void findId_Success_ReturnsMaskedEmail() {
        // given
        FindIdRequestDto request = FindIdRequestDto.builder()
                .nickname("뽀삐아빠")
                .build();

        User user = User.register("jjoodaeng2@gmail.com", "bcrypt_pwd_hash", "뽀삐아빠");

        given(userRepository.findFirstByNicknameAndStatus("뽀삐아빠", UserStatus.ACTIVE))
                .willReturn(Optional.of(user));

        // when
        FindIdResponseDto response = authService.findId(request);

        // then
        assertThat(response.getMaskedEmail()).isEqualTo("j*********@gmail.com");
    }

    @Test
    @DisplayName("일치하는 닉네임이 없으면 UserNotFoundException 발생")
    void findId_NicknameNotFound_ThrowsException() {
        // given
        FindIdRequestDto request = FindIdRequestDto.builder()
                .nickname("존재하지않는닉네임")
                .build();

        given(userRepository.findFirstByNicknameAndStatus("존재하지않는닉네임", UserStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.findId(request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("가입된 이메일로 비밀번호 찾기 요청 시 임시 비밀번호를 BCrypt 암호화하여 저장하고 메일을 발송한다")
    void findPassword_ExistingEmail_IssuesTemporaryPasswordAndSendsEmail() {
        // given
        FindPasswordRequestDto request = FindPasswordRequestDto.builder()
                .email("active@petspot.com")
                .build();

        User user = User.register("active@petspot.com", "old_bcrypt_hash", "활성유저");

        given(userRepository.findActiveByEmail("active@petspot.com")).willReturn(Optional.of(user));
        given(temporaryPasswordGenerator.generate()).willReturn("Temp1234!@#$");
        given(passwordEncoder.encode("Temp1234!@#$")).willReturn("new_bcrypt_temp_hash");

        // when
        authService.findPassword(request);

        // then: DB에는 BCrypt 해시만 저장되고 강제 변경 상태로 전환된다
        assertThat(user.getPasswordHash()).isEqualTo("new_bcrypt_temp_hash");
        assertThat(user.isPasswordChangeRequired()).isTrue();

        // 메일에는 평문 임시 비밀번호가 전달된다 (해시가 아님)
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendTemporaryPassword(org.mockito.ArgumentMatchers.eq("active@petspot.com"), passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isEqualTo("Temp1234!@#$");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 찾기 요청 시 예외 없이 조용히 종료하고 메일을 발송하지 않는다")
    void findPassword_NonExistentEmail_NoSideEffects() {
        // given
        FindPasswordRequestDto request = FindPasswordRequestDto.builder()
                .email("none@petspot.com")
                .build();

        given(userRepository.findActiveByEmail("none@petspot.com")).willReturn(Optional.empty());

        // when & then: 예외가 발생하지 않아야 한다 (계정 존재 여부 노출 방지)
        authService.findPassword(request);

        verify(emailSender, never()).sendTemporaryPassword(anyString(), anyString());
        verify(temporaryPasswordGenerator, never()).generate();
    }

    @Test
    @DisplayName("비밀번호 찾기 처리 중 평문 임시 비밀번호는 로그에 출력되지 않는다")
    void findPassword_TemporaryPasswordNeverAppearsInLogs() throws Exception {
        // given
        FindPasswordRequestDto request = FindPasswordRequestDto.builder()
                .email("active@petspot.com")
                .build();

        User user = User.register("active@petspot.com", "old_bcrypt_hash", "활성유저");
        String plainTemporaryPassword = "SuperSecret1!";

        given(userRepository.findActiveByEmail("active@petspot.com")).willReturn(Optional.of(user));
        given(temporaryPasswordGenerator.generate()).willReturn(plainTemporaryPassword);
        given(passwordEncoder.encode(plainTemporaryPassword)).willReturn("hashed_value");

        Logger authServiceLogger = (Logger) LoggerFactory.getLogger(AuthService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        authServiceLogger.addAppender(appender);
        authServiceLogger.setLevel(Level.ALL);

        try {
            // when
            authService.findPassword(request);

            // then
            List<String> logMessages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
            assertThat(logMessages).noneMatch(message -> message.contains(plainTemporaryPassword));
        } finally {
            authServiceLogger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("정상 비밀번호 변경 요청 시 새 비밀번호로 갱신되고 강제 변경 상태가 해제된다")
    void changePassword_Success() {
        // given
        User user = User.register("active@petspot.com", "old_bcrypt_hash", "활성유저");
        user.issueTemporaryPassword("temp_bcrypt_hash");
        UUID userId = user.getId();

        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .newPassword("NewPassw0rd!")
                .confirmPassword("NewPassw0rd!")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassw0rd!")).willReturn("new_bcrypt_hash");

        // when
        authService.changePassword(userId, request);

        // then
        assertThat(user.getPasswordHash()).isEqualTo("new_bcrypt_hash");
        assertThat(user.isPasswordChangeRequired()).isFalse();
    }

    @Test
    @DisplayName("새 비밀번호와 확인 비밀번호가 일치하지 않으면 IllegalArgumentException 발생")
    void changePassword_Mismatch_ThrowsException() {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .newPassword("NewPassw0rd!")
                .confirmPassword("Different1!")
                .build();

        // when & then
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 비밀번호 변경 시 UserNotFoundException 발생")
    void changePassword_UserNotFound_ThrowsException() {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                .newPassword("NewPassw0rd!")
                .confirmPassword("NewPassw0rd!")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(UserNotFoundException.class);
    }
}
