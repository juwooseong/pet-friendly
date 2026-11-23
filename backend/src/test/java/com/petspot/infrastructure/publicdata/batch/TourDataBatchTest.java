package com.petspot.infrastructure.publicdata.batch;

import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import com.petspot.infrastructure.publicdata.service.TourDataCollectorService;
import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService;
import com.petspot.infrastructure.publicdata.service.TourPlaceSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private TourPlaceSyncService tourPlaceSyncService;

    @Mock
    private TourPetPolicySyncService tourPetPolicySyncService;

    private TourDataCollectorService tourDataCollectorService;
    private TourDataBatch tourDataBatch;

    @BeforeEach
    void setUp() {
        tourDataCollectorService = new TourDataCollectorService(tourApiClient, tourApiPlaceMapper, placeRepository);
        tourDataBatch = new TourDataBatch(tourPlaceSyncService, tourPetPolicySyncService);
    }

    @Test
    @DisplayName("TourDataCollectorService 단일 페이지 수집 성공 및 DTO 매핑 검증")
    void collectPage_Success() {
        TourApiPlaceItemDto item = TourApiPlaceItemDto.builder()
                .contentId("1001")
                .title("댕댕이 힐링 공원")
                .address1("서울특별시 마포구")
                .rawLongitude("126.9")
                .rawLatitude("37.5")
                .build();
        given(tourApiClient.fetchPetPlaces(eq(10), eq(1))).willReturn(createMockResponse(List.of(item), 1, 1, 10));

        List<TourApiPlaceItemDto> items = tourDataCollectorService.collectPage(10, 1);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getContentId()).isEqualTo("1001");
        assertThat(items.get(0).getTitle()).isEqualTo("댕댕이 힐링 공원");
    }

    @Test
    @DisplayName("TourDataCollectorService 전체 수집 성공 검증")
    void collectAllPlaces_Success() {
        TourApiPlaceItemDto item1 = TourApiPlaceItemDto.builder().contentId("1001").title("장소 1").build();
        TourApiPlaceItemDto item2 = TourApiPlaceItemDto.builder().contentId("1002").title("장소 2").build();
        given(tourApiClient.fetchPetPlaces(eq(1), eq(1))).willReturn(createMockResponse(List.of(item1), 2, 1, 1));
        given(tourApiClient.fetchPetPlaces(eq(1), eq(2))).willReturn(createMockResponse(List.of(item2), 2, 2, 1));

        List<TourApiPlaceItemDto> result = tourDataCollectorService.collectAllPlaces(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("장소 1");
        assertThat(result.get(1).getTitle()).isEqualTo("장소 2");
    }

    @Test
    @DisplayName("배치 기본값은 비활성이며, 명시적으로 켜지 않으면 스케줄 실행 시 아무것도 호출하지 않는다")
    void runIngestionBatch_DisabledByDefault_DoesNothing() {
        tourDataBatch.runIngestionBatch();

        verifyNoInteractions(tourPlaceSyncService, tourPetPolicySyncService);
    }

    @Test
    @DisplayName("TSK-DATA-012: 장소 증분 동기화 후 정책 조회 단계를 설정된 호출 수로 실행한다")
    void runOnce_SyncsChangesThenPolicies() {
        given(tourPlaceSyncService.syncChanges(any(), isNull())).willReturn(placeResult(false));

        tourDataBatch.runOnce();

        verify(tourPlaceSyncService).syncChanges(any(LocalDate.class), isNull());
        verify(tourPetPolicySyncService).syncPendingPolicies(800, 100L);
    }

    @Test
    @DisplayName("TSK-DATA-012: 첫 실행 기준일(sync-since)을 설정하면 그 날짜를 동기화에 넘긴다")
    void runOnce_PassesSyncSince() {
        ReflectionTestUtils.setField(tourDataBatch, "syncSince", "20261119");
        given(tourPlaceSyncService.syncChanges(any(), any())).willReturn(placeResult(false));

        tourDataBatch.runOnce();

        verify(tourPlaceSyncService).syncChanges(any(LocalDate.class), eq(LocalDate.of(2026, 11, 19)));
    }

    @Test
    @DisplayName("장소 동기화에서 일일 호출 한도를 초과하면 정책 단계는 호출하지 않는다")
    void runOnce_QuotaExceededOnPlaceSync_SkipsPolicySync() {
        given(tourPlaceSyncService.syncChanges(any(), any())).willReturn(placeResult(true));

        TourDataBatch.BatchResult result = tourDataBatch.runOnce();

        assertThat(result.placeSync().quotaExceeded()).isTrue();
        verifyNoInteractions(tourPetPolicySyncService);
    }

    @Test
    @DisplayName("sync-place-list=false이면 장소 동기화 없이 정책 조회만 이어서 진행한다")
    void runOnce_PolicyOnly() {
        ReflectionTestUtils.setField(tourDataBatch, "syncPlaceList", false);

        tourDataBatch.runOnce();

        verifyNoInteractions(tourPlaceSyncService);
        verify(tourPetPolicySyncService).syncPendingPolicies(800, 100L);
    }

    private TourPlaceSyncService.SyncResult placeResult(boolean quotaExceeded) {
        return new TourPlaceSyncService.SyncResult("2026-11-19~2026-11-23", 5, 0, 0, 0, 0, 0,
                quotaExceeded ? "중단: 한도 초과" : null, quotaExceeded);
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
