package com.petspot.domain.favorite.repository;

import com.petspot.domain.favorite.entity.Favorite;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class FavoriteRepositoryTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    private User mockUser;
    private Place mockPlace;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        mockUser = User.register("user@petspot.com", "hash", "즐겨찾기유저");

        Point location = geometryFactory.createPoint(new Coordinate(127.0276, 37.4979));
        mockPlace = Place.builder()
                .id(UUID.randomUUID())
                .name("멍멍 애견카페")
                .category("CAFE")
                .categoryName("카페/음식점 > 애견카페")
                .address("서울시 강남구 테헤란로 123")
                .location(location)
                .latitude(37.4979)
                .longitude(127.0276)
                .build();
    }

    @Test
    @DisplayName("Favorite 엔티티 팩토리 생성 및 연관관계 매핑 검증")
    void favoriteEntity_Create_Success() {
        // given
        Favorite favorite = Favorite.create(mockUser, mockPlace);

        // then
        assertThat(favorite).isNotNull();
        assertThat(favorite.getUser()).isEqualTo(mockUser);
        assertThat(favorite.getPlace()).isEqualTo(mockPlace);
    }

    @Test
    @DisplayName("특정 사용자가 특정 장소를 즐겨찾기했는지 중복 확인 (existsByUserIdAndPlaceId) Mock 테스트")
    void existsByUserIdAndPlaceId_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        given(favoriteRepository.existsByUserIdAndPlaceId(userId, placeId)).willReturn(true);

        // when
        boolean exists = favoriteRepository.existsByUserIdAndPlaceId(userId, placeId);

        // then
        assertThat(exists).isTrue();
        verify(favoriteRepository).existsByUserIdAndPlaceId(userId, placeId);
    }

    @Test
    @DisplayName("특정 사용자의 즐겨찾기 목록 조회 (findAllByUserId) Mock 테스트")
    void findAllByUserId_Success() {
        // given
        UUID userId = mockUser.getId();
        Favorite favorite = Favorite.create(mockUser, mockPlace);

        given(favoriteRepository.findAllByUserId(userId)).willReturn(List.of(favorite));

        // when
        List<Favorite> favorites = favoriteRepository.findAllByUserId(userId);

        // then
        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).getPlace().getName()).isEqualTo("멍멍 애견카페");
        verify(favoriteRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("특정 사용자의 특정 장소 즐겨찾기 단건 조회 (findByUserIdAndPlaceId) Mock 테스트")
    void findByUserIdAndPlaceId_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();
        Favorite favorite = Favorite.create(mockUser, mockPlace);

        given(favoriteRepository.findByUserIdAndPlaceId(userId, placeId)).willReturn(Optional.of(favorite));

        // when
        Optional<Favorite> favoriteOpt = favoriteRepository.findByUserIdAndPlaceId(userId, placeId);

        // then
        assertThat(favoriteOpt).isPresent();
        assertThat(favoriteOpt.get().getPlace().getName()).isEqualTo("멍멍 애견카페");
    }

    @Test
    @DisplayName("특정 장소의 즐겨찾기 개수 조회 (countByPlaceId) Mock 테스트")
    void countByPlaceId_Success() {
        // given
        UUID placeId = mockPlace.getId();
        given(favoriteRepository.countByPlaceId(placeId)).willReturn(42L);

        // when
        long count = favoriteRepository.countByPlaceId(placeId);

        // then
        assertThat(count).isEqualTo(42L);
        verify(favoriteRepository).countByPlaceId(placeId);
    }

    @Test
    @DisplayName("특정 사용자의 특정 장소 즐겨찾기 삭제 (deleteByUserIdAndPlaceId) Mock 테스트")
    void deleteByUserIdAndPlaceId_Success() {
        // given
        UUID userId = mockUser.getId();
        UUID placeId = mockPlace.getId();

        // when
        favoriteRepository.deleteByUserIdAndPlaceId(userId, placeId);

        // then
        verify(favoriteRepository).deleteByUserIdAndPlaceId(userId, placeId);
    }
}
