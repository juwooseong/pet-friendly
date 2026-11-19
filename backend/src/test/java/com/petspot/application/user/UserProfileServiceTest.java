package com.petspot.application.user;

import com.petspot.global.error.exception.DuplicateNicknameException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
    @DisplayName("TSK-BE-033: 다른 회원이 사용 중인 닉네임으로 변경하면 DuplicateNicknameException이 발생하고 닉네임은 바뀌지 않는다")
    void updateProfile_DuplicateNickname_ThrowsAndKeepsNickname() {
        // given
        User user = User.register("user@petspot.com", "hash", "구닉네임", null);
        UUID userId = user.getId();
        UserProfileUpdateRequestDto updateRequest = UserProfileUpdateRequestDto.builder()
                .nickname("이미있는닉네임")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("이미있는닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userProfileService.updateProfile(userId, updateRequest))
                .isInstanceOf(DuplicateNicknameException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
        assertThat(user.getNickname()).isEqualTo("구닉네임");
    }

    @Test
    @DisplayName("TSK-BE-033: 현재 닉네임을 그대로 두고 프로필 이미지만 바꾸면 중복 검사 없이 성공한다")
    void updateProfile_SameNickname_SkipsDuplicateCheck() {
        // given
        User user = User.register("user@petspot.com", "hash", "구닉네임", null);
        UUID userId = user.getId();
        UserProfileUpdateRequestDto updateRequest = UserProfileUpdateRequestDto.builder()
                .nickname("구닉네임")
                .avatarUrl("https://example.com/new.png")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserProfileResponseDto response = userProfileService.updateProfile(userId, updateRequest);

        // then
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/new.png");
        verify(userRepository, never()).existsByNickname(anyString());
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
