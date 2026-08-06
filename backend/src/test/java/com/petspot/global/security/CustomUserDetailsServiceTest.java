package com.petspot.global.security;

import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("UUID 형식의 유효한 사용자 ID로 조회 성공 및 CustomUserDetails 반환")
    void loadUserByUsername_WithValidUUID_Success() {
        // given
        User activeUser = User.register("user@petspot.com", "pwd_hash", "유저");
        UUID userId = activeUser.getId();

        given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(userId.toString());
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("이메일 형식으로 조회 성공 및 CustomUserDetails 반환")
    void loadUserByUsername_WithEmail_Success() {
        // given
        String email = "emailuser@petspot.com";
        User activeUser = User.register(email, "pwd_hash", "이메일유저");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 UsernameNotFoundException 발생")
    void loadUserByUsername_NonExistentUser_ThrowsException() {
        // given
        String nonExistent = "nonexistent@petspot.com";
        given(userRepository.findByEmail(nonExistent)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistent))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("탈퇴한 회원(WITHDRAWN) 조회 시 UsernameNotFoundException 발생")
    void loadUserByUsername_WithdrawnUser_ThrowsException() {
        // given
        String email = "withdrawn@petspot.com";
        User withdrawnUser = User.register(email, "pwd_hash", "탈퇴유저");
        withdrawnUser.withdraw();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(withdrawnUser));

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("활성화된 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("비활성 회원(INACTIVE) 조회 시 UsernameNotFoundException 발생")
    void loadUserByUsername_InactiveUser_ThrowsException() {
        // given
        String email = "inactive@petspot.com";
        User inactiveUser = User.register(email, "pwd_hash", "비활성유저");
        inactiveUser.changeStatus(UserStatus.INACTIVE);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("활성화된 사용자를 찾을 수 없습니다.");
    }
}
