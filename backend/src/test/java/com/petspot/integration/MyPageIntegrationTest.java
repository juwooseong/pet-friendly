package com.petspot.integration;

import com.petspot.api.user.dto.MyPageResponseDto;
import com.petspot.application.user.MyPageQueryService;
import com.petspot.domain.favorite.entity.Favorite;
import com.petspot.domain.favorite.repository.FavoriteRepository;
import com.petspot.domain.pet.entity.Pet;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.repository.PetRepository;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.review.entity.Review;
import com.petspot.domain.review.repository.ReviewRepository;
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
class MyPageIntegrationTest {

    @Autowired
    private MyPageQueryService myPageQueryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("회원 등록 ➔ 펫 등록 ➔ 즐겨찾기 등록 ➔ 리뷰 작성 ➔ 마이페이지 통합 요약 조회 E2E 통합 테스트")
    void myPageSummary_IntegrationTest_Success() {
        // 1. 회원 등록
        User user = userRepository.save(User.register("e2e_user@petspot.com", "hash_password", "통합테스터"));

        // 2. 펫 2마리 등록 (첫 번째 펫은 대표 펫)
        Pet pet1 = petRepository.save(Pet.create(user, "대표개", "골든리트리버", PetGender.MALE, null, new BigDecimal("25.0"), true, true, null));
        Pet pet2 = petRepository.save(Pet.create(user, "서브개", "시츄", PetGender.FEMALE, null, new BigDecimal("4.0"), false, false, null));

        // 3. 장소 등록
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = gf.createPoint(new Coordinate(127.0, 37.5));
        Place place1 = placeRepository.save(Place.builder()
                .name("장소1")
                .category("CAFE")
                .categoryName("카페")
                .address("서울")
                .location(point)
                .latitude(37.5)
                .longitude(127.0)
                .build());

        Place place2 = placeRepository.save(Place.builder()
                .name("장소2")
                .category("PARK")
                .categoryName("공원")
                .address("서울")
                .location(point)
                .latitude(37.5)
                .longitude(127.0)
                .build());

        // 4. 즐겨찾기 2개 등록
        favoriteRepository.save(Favorite.create(user, place1));
        favoriteRepository.save(Favorite.create(user, place2));

        // 5. 리뷰 1개 등록
        reviewRepository.save(Review.create(user, place1, 5, "통합 리뷰 작성", null));

        // 6. 마이페이지 통합 요약 조회
        MyPageResponseDto summary = myPageQueryService.getMyPageSummary(user.getId());

        // then 검증
        assertThat(summary).isNotNull();
        assertThat(summary.getUserId()).isEqualTo(user.getId());
        assertThat(summary.getEmail()).isEqualTo("e2e_user@petspot.com");
        assertThat(summary.getNickname()).isEqualTo("통합테스터");
        assertThat(summary.getRepresentativePet()).isNotNull();
        assertThat(summary.getRepresentativePet().getName()).isEqualTo("대표개");
        assertThat(summary.getRepresentativePet().isRepresentative()).isTrue();
        assertThat(summary.getPetCount()).isEqualTo(2L);
        assertThat(summary.getFavoriteCount()).isEqualTo(2L);
        assertThat(summary.getReviewCount()).isEqualTo(1L);
    }
}
