package com.petspot.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.pet.dto.PetCreateRequestDto;
import com.petspot.api.review.dto.ReviewCreateRequestDto;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Sprint2FullFlowIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(Sprint2FullFlowIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Place mockPlace;

    @BeforeEach
    void setUp() {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = gf.createPoint(new Coordinate(127.0, 37.5));

        mockPlace = placeRepository.save(Place.builder()
                .name("스프린트2 테스트 테마 카페")
                .category("CAFE")
                .categoryName("카페")
                .address("서울시 강남구")
                .location(point)
                .latitude(37.5)
                .longitude(127.0)
                .build());

        entityManager.flush();
    }

    @Test
    @DisplayName("Sprint 2 전체 백엔드 E2E 통합 테스트 (회원가입 ➔ 로그인 ➔ JWT ➔ Pet ➔ 대표 설정 ➔ Favorite ➔ Review ➔ Place 갱신 ➔ Dashboard ➔ MyPage ➔ 삭제 Flow)")
    void sprint2_FullFlow_IntegrationTest_Success() throws Exception {

        // ==========================================
        // 1. 회원가입 (POST /api/v1/auth/register)
        // ==========================================
        UserRegisterRequestDto registerDto = UserRegisterRequestDto.builder()
                .email("sprint2_full@petspot.com")
                .password("Password123!")
                .nickname("스프린트2유저")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("sprint2_full@petspot.com")));

        // ==========================================
        // 2. 로그인 (POST /api/v1/auth/login) -> JWT 발급
        // ==========================================
        UserLoginRequestDto loginDto = UserLoginRequestDto.builder()
                .email("sprint2_full@petspot.com")
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        String accessToken = jsonNode.get("data").get("accessToken").asText();
        String authHeader = "Bearer " + accessToken;

        // ==========================================
        // 3. 사용자 정보 조회 (GET /api/v1/users/me)
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("sprint2_full@petspot.com")))
                .andExpect(jsonPath("$.data.petCount", is(0)));

        // ==========================================
        // 4. 반려동물 등록 (POST /api/v1/pets)
        // ==========================================
        PetCreateRequestDto petDto = PetCreateRequestDto.builder()
                .name("럭키")
                .breed("시베리안 허스키")
                .gender(PetGender.MALE)
                .birthDate(LocalDate.of(2021, 3, 15))
                .weightKg(new BigDecimal("22.5"))
                .neutered(true)
                .build();

        MvcResult petResult = mockMvc.perform(post("/api/v1/pets")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(petDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("럭키")))
                .andExpect(jsonPath("$.data.sizeCategory", is("LARGE")))
                .andReturn();

        String petIdStr = objectMapper.readTree(petResult.getResponse().getContentAsString()).get("data").get("id").asText();
        UUID petId = UUID.fromString(petIdStr);

        // ==========================================
        // 5. 대표 반려동물 설정 (PATCH /api/v1/pets/{petId}/representative)
        // ==========================================
        mockMvc.perform(patch("/api/v1/pets/" + petId + "/representative")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.representative", is(true)));

        // ==========================================
        // 6. 즐겨찾기 등록 (POST /api/v1/favorites/{placeId})
        // ==========================================
        mockMvc.perform(post("/api/v1/favorites/" + mockPlace.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.placeName", is("스프린트2 테스트 테마 카페")));

        // ==========================================
        // 7. 리뷰 등록 (POST /api/v1/reviews) -> Place rating/reviewCount 동기화
        // ==========================================
        ReviewCreateRequestDto reviewDto = ReviewCreateRequestDto.builder()
                .placeId(mockPlace.getId())
                .rating(5)
                .content("정말 친절하고 대형견도 편하게 쉴 수 있었습니다!")
                .build();

        MvcResult reviewResult = mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andReturn();

        String reviewIdStr = objectMapper.readTree(reviewResult.getResponse().getContentAsString()).get("data").get("id").asText();
        UUID reviewId = UUID.fromString(reviewIdStr);

        // ==========================================
        // 8. 장소 검색 (GET /api/v1/places/search)
        // ==========================================
        mockMvc.perform(get("/api/v1/places/search")
                        .param("keyword", "테스트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())));

        // ==========================================
        // 9. Dashboard 조회 (GET /api/v1/dashboard)
        // ==========================================
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.nickname", is("스프린트2유저")))
                .andExpect(jsonPath("$.data.representativePet.petName", is("럭키")))
                .andExpect(jsonPath("$.data.favoriteCount", is(1)));

        // ==========================================
        // 10. MyPage 조회 (GET /api/v1/users/me)
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.petCount", is(1)))
                .andExpect(jsonPath("$.data.favoriteCount", is(1)))
                .andExpect(jsonPath("$.data.reviewCount", is(1)));

        // ==========================================
        // 11. 즐겨찾기 목록 조회 (GET /api/v1/users/me/favorites)
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        // ==========================================
        // 12. 내 리뷰 목록 조회 (GET /api/v1/users/me/reviews)
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me/reviews")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        // ==========================================
        // 13. 삭제 Flow (리뷰 ➔ 즐겨찾기 ➔ 펫 삭제)
        // ==========================================
        // 리뷰 삭제
        mockMvc.perform(delete("/api/v1/reviews/" + reviewId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());

        // 즐겨찾기 삭제
        mockMvc.perform(delete("/api/v1/favorites/" + mockPlace.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());

        // 펫 삭제
        mockMvc.perform(delete("/api/v1/pets/" + petId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());

        // ==========================================
        // 14. 최종 삭제 상태 MyPage 재조회 검증 (petCount=0, favoriteCount=0, reviewCount=0)
        // ==========================================
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.petCount", is(0)))
                .andExpect(jsonPath("$.data.favoriteCount", is(0)))
                .andExpect(jsonPath("$.data.reviewCount", is(0)));
    }
}
