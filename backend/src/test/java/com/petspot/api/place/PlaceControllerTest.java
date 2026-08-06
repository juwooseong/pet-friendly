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
                BigDecimal.valueOf(15.0), 0.2
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
}
