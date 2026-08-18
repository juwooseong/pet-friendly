package com.petspot.api.place;

import com.petspot.application.place.PlaceQueryService;
import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaceController.class)
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaceQueryService placeQueryService;

    @MockBean
    private com.petspot.global.util.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.petspot.global.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.petspot.global.security.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/v1/places/search 정상 호출시 200 OK 및 PageResponse 반환")
    void searchPlaces_Success() throws Exception {
        // given
        PlaceSearchResponseDto mockDto = new PlaceSearchResponseDto(
                UUID.randomUUID(), "P-001", "홍대 애견카페", "CAFE", "카페",
                "서울특별시 마포구", 37.5567, 126.9236, "02-123-4567", "10:00-22:00",
                "https://example.com/image.jpg", BigDecimal.valueOf(4.5), 10,
                BigDecimal.valueOf(15.0), java.util.List.of("SMALL", "MEDIUM", "LARGE"), 0.2
        );

        PageResponse<PlaceSearchResponseDto> mockPageResponse = PageResponse.of(List.of(mockDto), 0, 20, 1L);

        given(placeQueryService.searchPlaces(any(PlaceSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/places/search")
                        .param("latitude", "37.5567")
                        .param("longitude", "126.9236")
                        .param("radiusKm", "3.0")
                        .param("category", "CAFE")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.totalPages", is(1)))
                .andExpect(jsonPath("$.data.hasNext", is(false)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("홍대 애견카페")))
                .andExpect(jsonPath("$.data.content[0].category", is("CAFE")));
    }

    @Test
    @DisplayName("유효하지 않은 latitude (범위 초과: 100.0) 입력시 400 Bad Request 반환 (Validation)")
    void searchPlaces_InvalidLatitude_BadRequest() throws Exception {
        // when & then: 위도가 90도를 초과할 경우 400 Bad Request
        mockMvc.perform(get("/api/v1/places/search")
                        .param("latitude", "100.0")
                        .param("longitude", "126.9236")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("유효하지 않은 radiusKm (음수: -5.0) 입력시 400 Bad Request 반환 (Validation)")
    void searchPlaces_InvalidRadius_BadRequest() throws Exception {
        // when & then: 반경 거리가 음수일 경우 400 Bad Request
        mockMvc.perform(get("/api/v1/places/search")
                        .param("radiusKm", "-5.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /api/v1/places/{placeId} 정상 호출시 200 OK 및 장소 상세 정보 반환")
    void getPlaceDetail_Success() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        PlaceSearchResponseDto mockDto = new PlaceSearchResponseDto(
                placeId, "P-001", "홍대 애견카페", "CAFE", "카페",
                "서울특별시 마포구", 37.5567, 126.9236, "02-123-4567", "10:00-22:00",
                "https://example.com/image.jpg", BigDecimal.valueOf(4.5), 10,
                BigDecimal.valueOf(15.0), java.util.List.of("SMALL", "MEDIUM", "LARGE"), null
        );

        given(placeQueryService.getPlaceDetail(placeId)).willReturn(mockDto);

        // when & then
        mockMvc.perform(get("/api/v1/places/{placeId}", placeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(placeId.toString())))
                .andExpect(jsonPath("$.data.name", is("홍대 애견카페")))
                .andExpect(jsonPath("$.data.address", is("서울특별시 마포구")))
                .andExpect(jsonPath("$.data.latitude", is(37.5567)))
                .andExpect(jsonPath("$.data.longitude", is(126.9236)))
                .andExpect(jsonPath("$.data.phone", is("02-123-4567")))
                .andExpect(jsonPath("$.data.imageUrl", is("https://example.com/image.jpg")))
                .andExpect(jsonPath("$.data.category", is("CAFE")))
                .andExpect(jsonPath("$.data.allowedSizes", hasSize(3)))
                .andExpect(jsonPath("$.data.rating", is(4.5)))
                .andExpect(jsonPath("$.data.reviewCount", is(10)));
    }

    @Test
    @DisplayName("GET /api/v1/places/{placeId} 존재하지 않는 ID 조회시 404 Not Found 반환")
    void getPlaceDetail_NotFound() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        given(placeQueryService.getPlaceDetail(placeId))
                .willThrow(new com.petspot.global.error.exception.PlaceNotFoundException("장소 정보를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/places/{placeId}", placeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error").exists());
    }
}
