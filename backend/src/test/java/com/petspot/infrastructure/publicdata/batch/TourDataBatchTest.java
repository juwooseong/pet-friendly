package com.petspot.infrastructure.publicdata.batch;

import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import com.petspot.infrastructure.publicdata.service.TourDataCollectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TourDataBatchTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourApiPlaceMapper tourApiPlaceMapper;

    @Mock
    private PlaceRepository placeRepository;

    private TourDataCollectorService tourDataCollectorService;
    private TourDataBatch tourDataBatch;

    @BeforeEach
    void setUp() {
        tourDataCollectorService = new TourDataCollectorService(tourApiClient, tourApiPlaceMapper, placeRepository);
        tourDataBatch = new TourDataBatch(tourDataCollectorService);
    }

    @Test
    @DisplayName("TourDataCollectorService 단일 페이지 수집 성공 및 DTO 매핑 검증")
    void collectPage_Success() {
        // given
        TourApiPlaceItemDto item = TourApiPlaceItemDto.builder()
                .contentId("1001")
                .title("댕댕이 힐링 공원")
                .address1("서울특별시 마포구")
                .longitude(126.9)
                .latitude(37.5)
                .build();

        TourApiResponseDto.Header header = TourApiResponseDto.Header.builder()
                .resultCode("0000")
                .resultMsg("OK")
                .build();

        TourApiResponseDto.Body body = TourApiResponseDto.Body.builder()
                .items(TourApiResponseDto.Items.builder().item(List.of(item)).build())
                .totalCount(1)
                .pageNo(1)
                .numOfRows(10)
                .build();

        TourApiResponseDto mockResponse = TourApiResponseDto.builder()
                .response(TourApiResponseDto.Response.builder().header(header).body(body).build())
                .build();

        given(tourApiClient.fetchPetPlaces(eq(10), eq(1))).willReturn(mockResponse);

        // when
        List<TourApiPlaceItemDto> items = tourDataCollectorService.collectPage(10, 1);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getContentId()).isEqualTo("1001");
        assertThat(items.get(0).getTitle()).isEqualTo("댕댕이 힐링 공원");
    }

    @Test
    @DisplayName("TourDataCollectorService 전체 수집 성공 검증")
    void collectAllPlaces_Success() {
        // given
        TourApiPlaceItemDto item1 = TourApiPlaceItemDto.builder().contentId("1001").title("장소 1").build();
        TourApiPlaceItemDto item2 = TourApiPlaceItemDto.builder().contentId("1002").title("장소 2").build();

        TourApiResponseDto response1 = createMockResponse(List.of(item1), 2, 1, 1);
        TourApiResponseDto response2 = createMockResponse(List.of(item2), 2, 2, 1);

        given(tourApiClient.fetchPetPlaces(eq(1), eq(1))).willReturn(response1);
        given(tourApiClient.fetchPetPlaces(eq(1), eq(2))).willReturn(response2);

        // when
        List<TourApiPlaceItemDto> result = tourDataCollectorService.collectAllPlaces(1);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("장소 1");
        assertThat(result.get(1).getTitle()).isEqualTo("장소 2");
    }

    @Test
    @DisplayName("TourDataBatch 스케줄러 실행 및 정상 동작 검증")
    void runIngestionBatch_Success() {
        // given
        TourApiPlaceItemDto item = TourApiPlaceItemDto.builder().contentId("1001").title("배치 수집 장소").build();
        TourApiResponseDto response = createMockResponse(List.of(item), 1, 1, 100);

        given(tourApiClient.fetchPetPlaces(anyInt(), anyInt())).willReturn(response);

        // when
        tourDataBatch.runIngestionBatch();

        // then
        verify(tourApiClient).fetchPetPlaces(100, 1);
    }

    @Test
    @DisplayName("API 호출 예외 발생 시 PublicDataException 처리 및 배치 무사 종료 검증")
    void runIngestionBatch_ApiException_HandledGracefully() {
        // given
        given(tourApiClient.fetchPetPlaces(anyInt(), anyInt()))
                .willThrow(new PublicDataException("TourAPI Timeout or Connection Error"));

        // when & then
        // 예외 발생 시 로그 출력 후 파이프라인 무사 종료 (Uncaught Exception으로 스케줄러가 죽지 않음)
        tourDataBatch.runIngestionBatch();

        verify(tourApiClient).fetchPetPlaces(100, 1);
    }

    private TourApiResponseDto createMockResponse(List<TourApiPlaceItemDto> items, int totalCount, int pageNo, int numOfRows) {
        TourApiResponseDto.Header header = TourApiResponseDto.Header.builder()
                .resultCode("0000")
                .resultMsg("OK")
                .build();

        TourApiResponseDto.Body body = TourApiResponseDto.Body.builder()
                .items(TourApiResponseDto.Items.builder().item(items).build())
                .totalCount(totalCount)
                .pageNo(pageNo)
                .numOfRows(numOfRows)
                .build();

        return TourApiResponseDto.builder()
                .response(TourApiResponseDto.Response.builder().header(header).body(body).build())
                .build();
    }
}
