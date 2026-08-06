package com.petspot.domain.user.repository;

import com.petspot.domain.user.entity.OAuthProvider;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("User.register 정적 팩토리 메서드로 일반 회원가입 객체 생성 및 기본값 검증")
    void userEntity_Register_Success() {
        // when
        User user = User.register("test@petspot.com", "hashed_password_1234", "초코아빠", "https://example.com/avatar.jpg");

        // then
        assertThat(user.getEmail()).isEqualTo("test@petspot.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed_password_1234");
        assertThat(user.getNickname()).isEqualTo("초코아빠");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getProvider()).isEqualTo(OAuthProvider.LOCAL);
    }

    @Test
    @DisplayName("User.registerOAuth 정적 팩토리 메서드로 카카오 소셜 회원가입 객체 생성 검증")
    void userEntity_RegisterOAuth_Success() {
        // when
        User user = User.registerOAuth("kakao_user@kakao.com", "카카오라이언", "https://example.com/kakao.jpg", OAuthProvider.KAKAO);

        // then
        assertThat(user.getEmail()).isEqualTo("kakao_user@kakao.com");
        assertThat(user.getNickname()).isEqualTo("카카오라이언");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getProvider()).isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    @DisplayName("이메일로 회원 존재 여부 확인 (existsByEmail) Mock 테스트")
    void existsByEmail_Success() {
        // given
        String email = "choco@petspot.com";
        given(userRepository.existsByEmail(email)).willReturn(true);

        // when
        boolean exists = userRepository.existsByEmail(email);

        // then
        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("이메일로 회원 조회 (findByEmail) Mock 테스트")
    void findByEmail_Success() {
        // given
        String email = "choco@petspot.com";
        User user = User.register(email, "hashed_pwd", "초코보호자");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        Optional<User> foundUserOpt = userRepository.findByEmail(email);

        // then
        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getEmail()).isEqualTo(email);
        assertThat(foundUserOpt.get().getNickname()).isEqualTo("초코보호자");
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("회원 탈퇴 처리 시 status=WITHDRAWN 및 withdrawAt 시간 기록 검증")
    void user_Withdraw_Success() {
        // given
        User user = User.register("withdraw@petspot.com", "pwd_hash", "탈퇴예정자");

        // when
        user.withdraw();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getWithdrawAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 탈퇴한 회원에게 중복 withdraw() 호출 시 IllegalStateException 예외 발생")
    void user_Withdraw_AlreadyWithdrawn_ThrowsException() {
        // given
        User user = User.register("withdraw@petspot.com", "pwd_hash", "탈퇴예정자");
        user.withdraw();

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(user::withdraw)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 탈퇴 처리된 회원입니다.");
    }

    @Test
    @DisplayName("활성 회원(ACTIVE) 상태만 조회하는 findByEmailAndStatus Mock 테스트")
    void findByEmailAndStatus_ActiveUser_Success() {
        // given
        String email = "active@petspot.com";
        User activeUser = User.register(email, "pwd_hash", "활성유저");
        given(userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE)).willReturn(Optional.of(activeUser));

        // when
        Optional<User> userOpt = userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE);

        // then
        assertThat(userOpt).isPresent();
        assertThat(userOpt.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).findByEmailAndStatus(email, UserStatus.ACTIVE);
    }
}
