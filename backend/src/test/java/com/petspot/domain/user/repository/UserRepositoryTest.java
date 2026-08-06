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
import java.util.UUID;

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
    @DisplayName("User Entity 도메인 객체 생성 및 기본값 검증")
    void userEntity_Creation_Success() {
        // given
        User user = User.builder()
                .email("test@petspot.com")
                .password("hashed_password_1234")
                .nickname("초코아빠")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .provider(OAuthProvider.LOCAL)
                .build();

        // then
        assertThat(user.getEmail()).isEqualTo("test@petspot.com");
        assertThat(user.getNickname()).isEqualTo("초코아빠");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getProvider()).isEqualTo(OAuthProvider.LOCAL);
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
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("hashed_pwd")
                .nickname("초코보호자")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        Optional<User> foundUserOpt = userRepository.findByEmail(email);

        // then
        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getEmail()).isEqualTo(email);
        assertThat(foundUserOpt.get().getNickname()).isEqualTo("초코보호자");
        verify(userRepository).findByEmail(email);
    }
}
