package com.petspot.integration;

import com.petspot.api.dashboard.dto.DashboardResponseDto;
import com.petspot.application.dashboard.DashboardQueryService;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DashboardIntegrationTest {

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Test
    @DisplayName("회원 등록 ➔ 대표 펫 등록 ➔ 장소 등록 ➔ 대시보드 조회 GET /api/v1/dashboard E2E 통합 테스트")
    void dashboardSummary_IntegrationTest_Success() {
        // 1. 회원가입 및 저장
        User user = userRepository.save(User.register("dash_integration@petspot.com", "password123!", "통합대시보드유저"));

        // 2. 대표 반려동물 등록
        Pet representativePet = petRepository.save(Pet.create(
                user, "대시개", "골든리트리버", PetGender.MALE, null, new BigDecimal("26.5"), true, true, null
        ));

        // 3. 인기 장소 및 최신 장소 등록
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = gf.createPoint(new Coordinate(127.03, 37.50));

        Place place1 = placeRepository.save(Place.builder()
                .name("인기 동반 카페")
                .category("CAFE")
                .categoryName("카페")
                .address("서울 강남구")
                .location(point)
                .latitude(37.50)
                .longitude(127.03)
                .rating(BigDecimal.valueOf(4.9))
                .reviewCount(25)
                .build());

        Place place2 = placeRepository.save(Place.builder()
                .name("신규 동반 운동장")
                .category("PARK")
                .categoryName("공원")
                .address("서울 서초구")
                .location(point)
                .latitude(37.51)
                .longitude(127.02)
                .rating(BigDecimal.valueOf(4.8))
                .reviewCount(5)
                .build());

        // 4. 대시보드 요약 정보 조회
        DashboardResponseDto dashboard = dashboardQueryService.getDashboardSummary(user.getId());

        // then 검증
        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getNickname()).isEqualTo("통합대시보드유저");
        assertThat(dashboard.getRepresentativePet()).isNotNull();
        assertThat(dashboard.getRepresentativePet().getPetName()).isEqualTo("대시개");
        assertThat(dashboard.getRepresentativePet().getBreed()).isEqualTo("골든리트리버");

        assertThat(dashboard.getPopularPlaces()).isNotEmpty();
        assertThat(dashboard.getPopularPlaces().get(0).getName()).isEqualTo("인기 동반 카페");

        assertThat(dashboard.getRecentPlaces()).isNotEmpty();
        assertThat(dashboard.getRecommendedPlaces()).isNotEmpty();
    }
}
