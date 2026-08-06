package com.petspot.application.auth;

import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

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
}
