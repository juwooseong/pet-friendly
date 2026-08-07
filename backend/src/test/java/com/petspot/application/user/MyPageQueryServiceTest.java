package com.petspot.application.user;

import com.petspot.api.user.dto.MyPageResponseDto;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.entity.PetSizeCategory;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.review.repository.ReviewRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class MyPageQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private MyPageQueryService myPageQueryService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = User.register("mypage@petspot.com", "hash", "마이페이지유저", "https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("마이페이지 통합 요약 정보 (유저 프로필, 대표 펫, 펫 수, 즐겨찾기 수, 리뷰 수) 정상 조회 성공")
    void getMyPageSummary_Success() {
        // given
        UUID userId = mockUser.getId();
        Pet mockRepPet = Pet.create(mockUser, "초코", "푸들", PetGender.MALE, null, new BigDecimal("5.5"), true, true, null);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.findRepresentativePet(userId)).willReturn(Optional.of(mockRepPet));
        given(petRepository.countByOwnerId(userId)).willReturn(2L);
        given(favoriteRepository.countByUserId(userId)).willReturn(5L);
        given(reviewRepository.countByUserIdAndDeletedFalse(userId)).willReturn(3L);

        // when
        MyPageResponseDto response = myPageQueryService.getMyPageSummary(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("mypage@petspot.com");
        assertThat(response.getNickname()).isEqualTo("마이페이지유저");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        assertThat(response.getRepresentativePet()).isNotNull();
        assertThat(response.getRepresentativePet().getPetName()).isEqualTo("초코");
        assertThat(response.getRepresentativePet().getBreed()).isEqualTo("푸들");
        assertThat(response.getRepresentativePet().getSizeCategory()).isEqualTo(PetSizeCategory.SMALL);
        assertThat(response.getPetCount()).isEqualTo(2L);
        assertThat(response.getFavoriteCount()).isEqualTo(5L);
        assertThat(response.getReviewCount()).isEqualTo(3L);

        verify(userRepository).findById(userId);
        verify(petRepository).findRepresentativePet(userId);
        verify(petRepository).countByOwnerId(userId);
        verify(favoriteRepository).countByUserId(userId);
        verify(reviewRepository).countByUserIdAndDeletedFalse(userId);
    }

    @Test
    @DisplayName("대표 반려동물이 없는 경우 representativePet 필드가 null로 정상 조회")
    void getMyPageSummary_NoRepresentativePet_Success() {
        // given
        UUID userId = mockUser.getId();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.findRepresentativePet(userId)).willReturn(Optional.empty());
        given(petRepository.countByOwnerId(userId)).willReturn(0L);
        given(favoriteRepository.countByUserId(userId)).willReturn(1L);
        given(reviewRepository.countByUserIdAndDeletedFalse(userId)).willReturn(0L);

        // when
        MyPageResponseDto response = myPageQueryService.getMyPageSummary(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRepresentativePet()).isNull();
        assertThat(response.getPetCount()).isEqualTo(0L);
        assertThat(response.getFavoriteCount()).isEqualTo(1L);
        assertThat(response.getReviewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 마이페이지 조회 시 UserNotFoundException 발생 (404)")
    void getMyPageSummary_NonExistentUser_ThrowsException() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        given(userRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> myPageQueryService.getMyPageSummary(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
}
