package com.petspot.application.user;

import com.petspot.api.user.dto.UserProfileResponseDto;
import com.petspot.api.user.dto.UserProfileUpdateRequestDto;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("정상 사용자 ID로 내 프로필 정보 조회 성공")
    void getMyProfile_Success() {
        // given
        User user = User.register("user@petspot.com", "hash", "초코아빠", "https://example.com/avatar.png");
        UUID userId = user.getId();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserProfileResponseDto response = userProfileService.getMyProfile(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("user@petspot.com");
        assertThat(response.getNickname()).isEqualTo("초코아빠");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("프로필 수정 요청 시 엔티티 도메인 메서드 호출을 통한 닉네임 및 아바타 URL 변경 성공")
    void updateProfile_Success() {
        // given
        User user = User.register("user@petspot.com", "hash", "구닉네임", "https://example.com/old.png");
        UUID userId = user.getId();

        UserProfileUpdateRequestDto updateRequest = UserProfileUpdateRequestDto.builder()
                .nickname("신규닉네임")
                .avatarUrl("https://example.com/new.png")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserProfileResponseDto response = userProfileService.updateProfile(userId, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("신규닉네임");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/new.png");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 프로필 조회 시 UserNotFoundException 발생")
    void getMyProfile_NonExistentUser_ThrowsException() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        given(userRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userProfileService.getMyProfile(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
}
