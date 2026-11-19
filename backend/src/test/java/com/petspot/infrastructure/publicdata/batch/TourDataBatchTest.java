package com.petspot.infrastructure.publicdata.batch;

import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import com.petspot.infrastructure.publicdata.service.TourDataCollectorService;
import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TourDataBatchTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourApiPlaceMapper tourApiPlaceMapper;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private TourPetPolicySyncService tourPetPolicySyncService;

    private TourDataCollectorService tourDataCollectorService;
    private TourDataBatch tourDataBatch;

    @BeforeEach
    void setUp() {
        tourDataCollectorService = new TourDataCollectorService(tourApiClient, tourApiPlaceMapper, placeRepository);
        tourDataBatch = new TourDataBatch(tourDataCollectorService, tourPetPolicySyncService);
    }

    @Test
    @DisplayName("TourDataCollectorService 단일 페이지 수집 성공 및 DTO 매핑 검증")
    void collectPage_Success() {
        // given
        TourApiPlaceItemDto item = TourApiPlaceItemDto.builder()
                .contentId("1001")
                .title("댕댕이 힐링 공원")
                .address1("서울특별시 마포구")
                .rawLongitude("126.9")
                .rawLatitude("37.5")
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
        // given: 배치 기본값은 비활성(false)이므로, 실행 로직 검증을 위해 명시적으로 활성화한다.
        ReflectionTestUtils.setField(tourDataBatch, "batchEnabled", true);
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
        // given: 배치를 명시적으로 활성화한 상태에서 API 예외 발생
        ReflectionTestUtils.setField(tourDataBatch, "batchEnabled", true);
        given(tourApiClient.fetchPetPlaces(anyInt(), anyInt()))
                .willThrow(new PublicDataException("TourAPI Timeout or Connection Error"));

        // when & then
        // 예외 발생 시 로그 출력 후 파이프라인 무사 종료 (Uncaught Exception으로 스케줄러가 죽지 않음)
        tourDataBatch.runIngestionBatch();

        verify(tourApiClient).fetchPetPlaces(100, 1);
    }

    @Test
    @DisplayName("배치 기본값은 비활성이며, 명시적으로 켜지 않으면 스케줄 실행 시 TourAPI를 호출하지 않는다")
    void runIngestionBatch_DisabledByDefault_DoesNotCallApi() {
        // TSK-DATA-003-C(전국 적재: 속도 제한/일일 한도 대응) 설계 전 의도치 않은 전체 수집을 막기 위한 안전장치 검증
        // when
        tourDataBatch.runIngestionBatch();

        // then
        verifyNoInteractions(tourApiClient);
    }

    @Test
    @DisplayName("TSK-DATA-010: 목록을 페이지 단위로 저장한 뒤 정책 조회 단계를 설정된 호출 수로 실행한다")
    void runOnce_SavesListThenSyncsPolicies() {
        // given: 마지막 페이지(페이지 크기보다 적은 1건)
        TourApiPlaceItemDto item = TourApiPlaceItemDto.builder().contentId("1001").title("장소").build();
        given(tourApiClient.fetchPetPlaces(100, 1)).willReturn(createMockResponse(List.of(item), 1, 1, 100));

        // when
        tourDataBatch.runOnce();

        // then: 1페이지만 호출(마지막 페이지 감지)하고 정책 단계는 기본 800건/100ms로 실행
        verify(tourApiClient).fetchPetPlaces(100, 1);
        verify(tourPetPolicySyncService).syncPendingPolicies(800, 100L);
    }

    @Test
    @DisplayName("TSK-DATA-010: 목록 단계에서 일일 호출 한도를 초과하면 정책 단계는 호출하지 않는다")
    void runOnce_QuotaExceededOnList_SkipsPolicySync() {
        given(tourApiClient.fetchPetPlaces(anyInt(), anyInt()))
                .willThrow(new PublicDataException("TourAPI 게이트웨이 오류: [22] LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"));

        TourDataBatch.BatchResult result = tourDataBatch.runOnce();

        assertThat(result.placeList().quotaExceeded()).isTrue();
        verifyNoInteractions(tourPetPolicySyncService);
    }

    @Test
    @DisplayName("TSK-DATA-010: sync-place-list=false이면 목록 호출 없이 정책 조회만 이어서 진행한다")
    void runOnce_PolicyOnly() {
        ReflectionTestUtils.setField(tourDataBatch, "syncPlaceList", false);

        tourDataBatch.runOnce();

        verifyNoInteractions(tourApiClient);
        verify(tourPetPolicySyncService).syncPendingPolicies(800, 100L);
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
