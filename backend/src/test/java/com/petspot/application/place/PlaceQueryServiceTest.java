package com.petspot.application.place;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class PlaceQueryServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceQueryService placeQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("PlaceQueryService 검색 요청 시 PlaceRepository.searchPlaces 호출 및 DTO 반환 검증")
    void searchPlaces_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(37.5567)
                .longitude(126.9236)
                .radiusKm(3.0)
                .category("CAFE")
                .build();

        PlaceSearchResponseDto expectedDto = new PlaceSearchResponseDto(
                UUID.randomUUID(), "P-001", "홍대 애견카페", "CAFE", "카페",
                "서울특별시 마포구", 37.5567, 126.9236, "02-123-4567", "10:00-22:00",
                "https://example.com/image.jpg", BigDecimal.valueOf(4.5), 10,
                BigDecimal.valueOf(15.0), java.util.List.of("SMALL", "MEDIUM", "LARGE"), 0.1
        );

        given(placeRepository.searchPlaces(condition)).willReturn(List.of(expectedDto));

        // when
        List<PlaceSearchResponseDto> results = placeQueryService.searchPlaces(condition);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("홍대 애견카페");
        verify(placeRepository).searchPlaces(condition);
    }

    @Test
    @DisplayName("PlaceQueryService Pageable 검색 요청 시 PlaceRepository.searchPlaces(condition, pageable) 호출 및 PageResponse 반환 검증")
    void searchPlaces_Pageable_Success() {
        // given
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(37.5567)
                .longitude(126.9236)
                .radiusKm(3.0)
                .category("CAFE")
                .build();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        PlaceSearchResponseDto expectedDto = new PlaceSearchResponseDto(
                UUID.randomUUID(), "P-001", "홍대 애견카페", "CAFE", "카페",
                "서울특별시 마포구", 37.5567, 126.9236, "02-123-4567", "10:00-22:00",
                "https://example.com/image.jpg", BigDecimal.valueOf(4.5), 10,
                BigDecimal.valueOf(15.0), java.util.List.of("SMALL", "MEDIUM", "LARGE"), 0.1
        );

        org.springframework.data.domain.Page<PlaceSearchResponseDto> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(expectedDto), pageable, 1L);

        given(placeRepository.searchPlaces(condition, pageable)).willReturn(mockPage);

        // when
        com.petspot.global.dto.PageResponse<PlaceSearchResponseDto> result = placeQueryService.searchPlaces(condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        verify(placeRepository).searchPlaces(condition, pageable);
    }
}
