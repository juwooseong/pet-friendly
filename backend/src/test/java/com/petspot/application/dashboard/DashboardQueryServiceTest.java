package com.petspot.application.dashboard;

import com.petspot.api.dashboard.dto.DashboardResponseDto;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class DashboardQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private DashboardQueryService dashboardQueryService;

    private User mockUser;
    private Place mockPlace1;
    private Place mockPlace2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = User.register("dashboard@petspot.com", "hash", "대시보드유저");
        mockPlace1 = Place.builder().id(UUID.randomUUID()).name("인기 카페").rating(BigDecimal.valueOf(4.9)).reviewCount(10).build();
        mockPlace2 = Place.builder().id(UUID.randomUUID()).name("최신 공원").rating(BigDecimal.valueOf(4.5)).reviewCount(2).build();
    }

    @Test
    @DisplayName("대시보드 통합 요약 정보 (유저 요약, 대표 펫, 즐겨찾기 수, 인기 장소, 최신 장소, 추천 장소 Top10) 정상 조회 성공")
    void getDashboardSummary_Success() {
        // given
        UUID userId = mockUser.getId();
        Pet mockRepPet = Pet.create(mockUser, "뽀삐", "말티즈", PetGender.FEMALE, null, new BigDecimal("3.2"), true, true, null);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.findRepresentativePet(userId)).willReturn(Optional.of(mockRepPet));
        given(favoriteRepository.countByUserId(userId)).willReturn(7L);
        given(placeRepository.findTop10ByOrderByRatingDescReviewCountDesc()).willReturn(List.of(mockPlace1));
        given(placeRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(List.of(mockPlace2));

        // when
        DashboardResponseDto response = dashboardQueryService.getDashboardSummary(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("대시보드유저");
        assertThat(response.getRepresentativePet()).isNotNull();
        assertThat(response.getRepresentativePet().getPetName()).isEqualTo("뽀삐");
        assertThat(response.getFavoriteCount()).isEqualTo(7L);
        assertThat(response.getPopularPlaces()).hasSize(1);
        assertThat(response.getPopularPlaces().get(0).getName()).isEqualTo("인기 카페");
        assertThat(response.getRecentPlaces()).hasSize(1);
        assertThat(response.getRecentPlaces().get(0).getName()).isEqualTo("최신 공원");
        assertThat(response.getRecommendedPlaces()).hasSize(1);

        verify(userRepository).findById(userId);
        verify(petRepository).findRepresentativePet(userId);
        verify(favoriteRepository).countByUserId(userId);
        verify(placeRepository).findTop10ByOrderByRatingDescReviewCountDesc();
        verify(placeRepository).findTop10ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("등록된 장소가 아무것도 없는 경우 대시보드 리스트가 빈 배열로 안전하게 반환 성공")
    void getDashboardSummary_EmptyData_Success() {
        // given
        UUID userId = mockUser.getId();
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.findRepresentativePet(userId)).willReturn(Optional.empty());
        given(favoriteRepository.countByUserId(userId)).willReturn(0L);
        given(placeRepository.findTop10ByOrderByRatingDescReviewCountDesc()).willReturn(Collections.emptyList());
        given(placeRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(Collections.emptyList());

        // when
        DashboardResponseDto response = dashboardQueryService.getDashboardSummary(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRepresentativePet()).isNull();
        assertThat(response.getPopularPlaces()).isEmpty();
        assertThat(response.getRecentPlaces()).isEmpty();
        assertThat(response.getRecommendedPlaces()).isEmpty();
    }

    @Test
    @DisplayName("대표 반려동물이 없는 유저인 경우 representativePet 필드가 null로 반환")
    void getDashboardSummary_NoRepresentativePet_Success() {
        // given
        UUID userId = mockUser.getId();
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(petRepository.findRepresentativePet(userId)).willReturn(Optional.empty());
        given(favoriteRepository.countByUserId(userId)).willReturn(3L);
        given(placeRepository.findTop10ByOrderByRatingDescReviewCountDesc()).willReturn(List.of(mockPlace1));
        given(placeRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(List.of(mockPlace2));

        // when
        DashboardResponseDto response = dashboardQueryService.getDashboardSummary(userId);

        // then
        assertThat(response.getRepresentativePet()).isNull();
        assertThat(response.getFavoriteCount()).isEqualTo(3L);
    }
}
