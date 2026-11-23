package com.petspot.integration;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.entity.PublicDataSyncState;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.domain.place.repository.PublicDataSyncStateRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.service.TourPlaceSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * TourAPI 증분 동기화 실DB 통합 테스트 (TSK-DATA-012). TourAPI 호출만 Mock이고 저장·검색·대시보드 쿼리는 실제 PostGIS에서 실행한다.
 */
@SpringBootTest
@Transactional
class TourPlaceSyncIntegrationTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    // 실제 장소가 없는 서해 바다 좌표 (RULES.md 2.3 테스트 데이터 격리)
    private static final double LAT = 34.0067;
    private static final double LON = 124.5236;
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MODIFIED = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private TourPlaceSyncService syncService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PublicDataSyncStateRepository stateRepository;

    @MockBean
    private TourApiClient tourApiClient;

    private final LocalDate today = LocalDate.now(KOREA);

    @Test
    @DisplayName("바뀐 장소는 정책·상세를 다시 조회하게 하고, 같은 수정 시각을 다시 받으면 그대로 두며, 새 장소는 등록하고 숨긴 새 장소는 등록하지 않는다")
    void syncChanges_ChangedNewAndHiddenNew() {
        String id = "sync-" + UUID.randomUUID();
        Place existing = saveSyncedPlace(id + "-a", id + "-기존");
        String later = ZonedDateTime.now(KOREA).plusHours(1).format(MODIFIED); // 적재 이후 수정

        givenItems(item(id + "-a", "1", later), item(id + "-b", "1", later), item(id + "-c", "0", later));

        TourPlaceSyncService.SyncResult first = syncService.syncChanges(today, today);

        assertThat(first.stopReason()).isNull();
        assertThat(first.changed()).isEqualTo(1);
        assertThat(first.created()).isEqualTo(1);
        assertThat(existing.getPetPolicySyncedAt()).isNull(); // 정책 배치가 다시 조회
        assertThat(existing.getDetailSyncedAt()).isNull();    // 상세 화면이 다시 조회
        assertThat(placeRepository.findByPublicDataId(id + "-b")).isPresent();
        assertThat(placeRepository.findByPublicDataId(id + "-c")).isEmpty();
        assertThat(stateRepository.findById(PublicDataSyncState.TOUR_PET_PLACES))
                .get().extracting(PublicDataSyncState::getLastSyncedDate).isEqualTo(today);

        // 다음 실행이 같은 날을 다시 조회해도(하루 겹침) 이미 반영한 수정 시각이면 재조회 표시를 하지 않는다
        existing.markTourApiPetPolicyNotProvided(ZonedDateTime.now(KOREA));
        TourPlaceSyncService.SyncResult second = syncService.syncChanges(today, null);

        assertThat(second.changed()).isZero();
        assertThat(existing.getPetPolicySyncedAt()).isNotNull();
    }

    @Test
    @DisplayName("TourAPI가 숨긴 장소는 검색·홈·정책 조회 대상에서 빠지고, 다시 노출되면 돌아온다")
    void syncChanges_HiddenPlaceExcludedThenRestored() {
        String id = "sync-" + UUID.randomUUID();
        Place place = placeRepository.saveAndFlush(newPlace(id + "-h", id + "-숨김")); // 정책 미조회 상태
        String modified = ZonedDateTime.now(KOREA).minusDays(1).format(MODIFIED);
        // 동기화가 이름을 TourAPI 제목(테스트에선 contentId)으로 갱신하므로 둘 다 포함하는 id로 검색
        PlaceSearchCondition byKeyword = PlaceSearchCondition.builder().keyword(id).build();
        assertThat(placeRepository.findPendingPetPolicySync(PageRequest.of(0, 100000))).contains(place);

        givenItems(item(id + "-h", "0", modified));
        assertThat(syncService.syncChanges(today, today).hidden()).isEqualTo(1);

        assertThat(place.getHiddenAt()).isNotNull();
        assertThat(placeRepository.searchPlaces(byKeyword, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(placeRepository.findPopularPlaces(PageRequest.of(0, 100000))).doesNotContain(place);
        assertThat(placeRepository.findRecentPlaces(PageRequest.of(0, 100000))).doesNotContain(place);
        assertThat(placeRepository.findPendingPetPolicySync(PageRequest.of(0, 100000))).doesNotContain(place);
        assertThat(placeRepository.findById(place.getId())).isPresent(); // 리뷰·즐겨찾기 연결 때문에 지우지 않는다

        givenItems(item(id + "-h", "1", modified));
        syncService.syncChanges(today, null);

        assertThat(place.getHiddenAt()).isNull();
        assertThat(placeRepository.searchPlaces(byKeyword, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("중간 날짜 조회가 실패하면 진행 날짜를 옮기지 않아 다음 실행이 같은 날짜부터 다시 받는다")
    void syncChanges_FailureKeepsProgress() {
        LocalDate yesterday = today.minusDays(1);
        given(tourApiClient.fetchPetPlaceChanges(anyInt(), anyInt(), eq(yesterday.format(DateTimeFormatter.BASIC_ISO_DATE))))
                .willReturn(response(List.of()));
        given(tourApiClient.fetchPetPlaceChanges(anyInt(), anyInt(), eq(today.format(DateTimeFormatter.BASIC_ISO_DATE))))
                .willThrow(new PublicDataException("TourAPI 게이트웨이 오류: [22] LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"));

        TourPlaceSyncService.SyncResult result = syncService.syncChanges(today, yesterday);

        assertThat(result.calls()).isEqualTo(1);
        assertThat(result.quotaExceeded()).isTrue();
        assertThat(stateRepository.findById(PublicDataSyncState.TOUR_PET_PLACES)).isEmpty();
    }

    private Place saveSyncedPlace(String contentId, String name) {
        Place place = placeRepository.saveAndFlush(newPlace(contentId, name));
        ZonedDateTime now = ZonedDateTime.now(KOREA);
        place.markTourApiPetPolicyNotProvided(now);
        place.applyTourApiDetail(null, null, null, null, now);
        return place;
    }

    private Place newPlace(String contentId, String name) {
        return Place.builder().publicDataId(contentId).name(name).category("PARK").address("테스트 주소")
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(LON, LAT))).latitude(LAT).longitude(LON).build();
    }

    private void givenItems(TourApiPlaceItemDto... items) {
        given(tourApiClient.fetchPetPlaceChanges(anyInt(), anyInt(), any())).willReturn(response(List.of(items)));
    }

    private TourApiPlaceItemDto item(String contentId, String showFlag, String modifiedTime) {
        return TourApiPlaceItemDto.builder().contentId(contentId).contentTypeId("12").title(contentId)
                .address1("테스트 주소").rawLatitude(String.valueOf(LAT)).rawLongitude(String.valueOf(LON))
                .modifiedTime(modifiedTime).showFlag(showFlag).build();
    }

    private TourApiResponseDto response(List<TourApiPlaceItemDto> items) {
        return TourApiResponseDto.builder()
                .response(TourApiResponseDto.Response.builder()
                        .header(TourApiResponseDto.Header.builder().resultCode("0000").resultMsg("OK").build())
                        .body(TourApiResponseDto.Body.builder()
                                .items(TourApiResponseDto.Items.builder().item(items).build())
                                .totalCount(items.size()).pageNo(1).numOfRows(100).build())
                        .build())
                .build();
    }
}
