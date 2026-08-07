package com.petspot.application.favorite;

import com.petspot.api.favorite.dto.FavoriteResponseDto;
import com.petspot.domain.favorite.entity.Favorite;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.repository.UserRepository;
import com.petspot.global.error.exception.DuplicateFavoriteException;
import com.petspot.global.error.exception.FavoriteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User mockUser;
    private Place mockPlace;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = User.register("favorite@petspot.com", "hash", "즐겨찾기유저");
        mockPlace = Place.builder().id(UUID.randomUUID()).name("동반 식당").build();
    }

    @Test
    @DisplayName("즐겨찾기 등록 성공")
    void addFavorite_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(placeRepository.findById(placeId)).willReturn(Optional.of(mockPlace));
        given(favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(false);

        Favorite favorite = Favorite.create(mockUser, mockPlace);
        given(favoriteRepository.save(any(Favorite.class))).willReturn(favorite);

        // when
        FavoriteResponseDto response = favoriteService.addFavorite(userId, placeId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPlaceId()).isEqualTo(placeId);
        assertThat(response.getPlaceName()).isEqualTo("동반 식당");
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("이미 등록된 즐겨찾기 중복 등록 시 DuplicateFavoriteException 예외 발생 (409)")
    void addFavorite_Duplicate_ThrowsException() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(placeRepository.findById(placeId)).willReturn(Optional.of(mockPlace));
        given(favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> favoriteService.addFavorite(userId, placeId))
                .isInstanceOf(DuplicateFavoriteException.class)
                .hasMessage("이미 즐겨찾기에 등록된 장소입니다.");
    }

    @Test
    @DisplayName("즐겨찾기 해제(삭제) 성공")
    void removeFavorite_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        given(favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(true);

        // when
        favoriteService.removeFavorite(userId, placeId);

        // then
        verify(favoriteRepository).deleteByUserIdAndPlaceId(userId, placeId);
    }

    @Test
    @DisplayName("존재하지 않는 즐겨찾기 해제 시도 시 FavoriteNotFoundException 예외 발생 (404)")
    void removeFavorite_NotFound_ThrowsException() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        given(favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> favoriteService.removeFavorite(userId, placeId))
                .isInstanceOf(FavoriteNotFoundException.class)
                .hasMessage("즐겨찾기 항목을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("내 즐겨찾기 목록 조회 성공")
    void getMyFavorites_Success() {
        // given
        UUID userId = mockUser.getId();
        Favorite favorite = Favorite.create(mockUser, mockPlace);

        given(favoriteRepository.findAllByUserId(userId)).willReturn(List.of(favorite));

        // when
        List<FavoriteResponseDto> response = favoriteService.getMyFavorites(userId);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getPlaceName()).isEqualTo("동반 식당");
    }
}
