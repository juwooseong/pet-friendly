package com.petspot.infrastructure.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

/**
 * TSK-DATA-003-A: TourAPI 초기 데이터 적재 최소 파이프라인(실제 DB 저장) 검증.
 *
 * <p>실제 API는 호출하지 않는다({@link TourApiClient}를 Mock으로 대체) - TSK-DATA-005에서 확보한 실제
 * {@code areaBasedList2} fixture(serviceKey 미포함)를 사용해, 기존 {@link TourDataCollectorService}
 * 파이프라인(신규 코드 없이 기존 {@code collectPage}/{@code saveCollectedPlaces} 재사용)이 실제 로컬
 * PostgreSQL/PostGIS에 정상 저장되는지 검증한다. 실제 API를 호출하는 통합 테스트는 별도
 * {@link TourDataInitialIngestionRealApiIntegrationTest}에서 다룬다(기본 스킵).
 */
@SpringBootTest
@Transactional
class TourDataInitialIngestionPipelineTest {

    @Autowired
    private TourDataCollectorService collectorService;

    @Autowired
    private PlaceRepository placeRepository;

    @MockBean
    private TourApiClient tourApiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("실제 areaBasedList2 fixture: 역직렬화 -> DTO -> Place 매핑 -> DB 저장까지 전체 파이프라인이 성공하고, 반려동물 정책 5개 필드는 전부 null이다")
    void realFixture_EndToEndIngestion_PolicyFieldsAreNull() throws IOException {
        // given: TSK-DATA-005에서 확보한 실제 성공 응답(serviceKey 미포함)
        TourApiResponseDto realResponse = readRealFixture();
        List<TourApiPlaceItemDto> items = realResponse.getResponse().getBody().getItemList();
        assertThat(items).as("실제 fixture에 최소 1건의 item이 있어야 한다").isNotEmpty();

        given(tourApiClient.fetchPetPlaces(anyInt(), anyInt())).willReturn(realResponse);

        // when: 기존 파이프라인 재사용 (신규 API 없음) - 최대 5건 수집 후 저장
        List<TourApiPlaceItemDto> collected = collectorService.collectPage(5, 1);
        int[] result = collectorService.saveCollectedPlaces(collected);

        // then: 신규 저장 건수 검증
        assertThat(result[0]).isEqualTo(collected.size());
        assertThat(result[1]).isZero();

        TourApiPlaceItemDto firstDto = items.get(0);
        Optional<Place> savedOpt = placeRepository.findByPublicDataId(firstDto.getContentId());
        assertThat(savedOpt).isPresent();
        Place saved = savedOpt.get();

        // 장소 기본정보는 정상 저장되어야 한다
        assertThat(saved.getPublicDataId()).isEqualTo("2930927");
        assertThat(saved.getName()).isEqualTo("가까운약국");
        assertThat(saved.getAddress()).contains("부산광역시 부산진구");
        assertThat(saved.getLongitude()).isEqualTo(129.056805686045);
        assertThat(saved.getLatitude()).isEqualTo(35.1579550616706);
        assertThat(saved.getImageUrl()).isEqualTo("https://tong.visitkorea.or.kr/cms/resource/90/4017190_image2_1.jpg");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // 반려동물 정책 5개 필드는 TourAPI 근거가 없으므로 전부 null이어야 한다(TSK-DATA-008 원칙)
        assertThat(saved.getMaxWeightLimitKg()).isNull();
        assertThat(saved.getAllowedSizes()).isNull();
        assertThat(saved.getIsIndoorAllowed()).isNull();
        assertThat(saved.getIsOutdoorAllowed()).isNull();
        assertThat(saved.getHasOffLeashZone()).isNull();
    }

    @Test
    @DisplayName("동일한 5건을 두 번 적재해도 Place가 중복 생성되지 않고 기존 findByPublicDataId 기반 중복 방지가 그대로 적용된다")
    void sameBatch_ProcessedTwice_DoesNotDuplicate() throws IOException {
        // given
        TourApiResponseDto realResponse = readRealFixture();
        given(tourApiClient.fetchPetPlaces(anyInt(), anyInt())).willReturn(realResponse);

        List<TourApiPlaceItemDto> collected = collectorService.collectPage(5, 1);
        long countBefore = placeRepository.count();

        // when: 최초 적재
        int[] firstRun = collectorService.saveCollectedPlaces(collected);
        long countAfterFirst = placeRepository.count();

        // when: 동일 데이터 재적재
        int[] secondRun = collectorService.saveCollectedPlaces(collected);
        long countAfterSecond = placeRepository.count();

        // then: 최초 실행은 신규 저장, 재실행은 업데이트 처리(중복 미생성)
        assertThat(firstRun[0]).isEqualTo(collected.size());
        assertThat(firstRun[1]).isZero();
        assertThat(secondRun[0]).isZero();
        assertThat(secondRun[1]).isEqualTo(collected.size());

        assertThat(countAfterFirst - countBefore).isEqualTo(collected.size());
        assertThat(countAfterSecond).isEqualTo(countAfterFirst); // 재실행 후에도 레코드 수 불변
    }

    /**
     * TSK-DATA-003-B (ADR-008, 전략 B): 좌표가 없는 장소는 적재하지 않는다(Place.latitude/longitude/
     * location이 DB NOT NULL이라 임의 좌표 없이는 저장할 방법이 없고, 0.0/0.0(Null Island) 같은 임의
     * 좌표로 대체해 지도/거리 검색에 잘못된 위치로 노출시키지 않기 위함). 좌표 결측 항목은 건너뛰고
     * 나머지 정상 항목은 그대로 저장되어야 한다.
     */
    @Test
    @DisplayName("좌표가 빈 문자열이거나 파싱 불가능한 항목이 섞여 있어도 배치 전체가 실패하지 않고, 해당 항목만 스킵되고 정상 좌표 항목만 저장된다")
    void mixedBatch_WithInvalidCoordinates_SkipsOnlyInvalidItems() {
        // given: 정상 좌표 1건 + 좌표 빈 문자열 1건 + 좌표 파싱 불가 1건
        TourApiPlaceItemDto validItem = TourApiPlaceItemDto.builder()
                .contentId("VALID-1")
                .title("정상 좌표 장소")
                .address1("서울특별시 마포구")
                .rawLongitude("126.9")
                .rawLatitude("37.5")
                .build();

        TourApiPlaceItemDto blankCoordItem = TourApiPlaceItemDto.builder()
                .contentId("BLANK-COORD-1")
                .title("좌표 빈 문자열 장소")
                .address1("경기도 용인시")
                .rawLongitude("")
                .rawLatitude("")
                .build();

        TourApiPlaceItemDto malformedCoordItem = TourApiPlaceItemDto.builder()
                .contentId("BAD-COORD-1")
                .title("좌표 파싱 불가 장소")
                .address1("인천광역시")
                .rawLongitude("N/A")
                .rawLatitude("모름")
                .build();

        // when: 예외 없이 3건 모두 처리되지만(배치가 깨지지 않음), 좌표 없는 2건은 저장에서 제외된다
        int[] result = collectorService.saveCollectedPlaces(List.of(validItem, blankCoordItem, malformedCoordItem));

        // then: 정상 좌표를 가진 1건만 신규 저장된다
        assertThat(result[0]).isEqualTo(1);
        assertThat(result[1]).isZero();
        assertThat(placeRepository.findByPublicDataId("VALID-1")).isPresent();

        // 좌표 결측 항목은 임의 좌표(0.0/0.0 등)로도 저장되지 않고, 아예 DB에 존재하지 않아야 한다
        // (지도/거리 검색에 잘못된 위치로 노출되는 것을 원천 차단)
        assertThat(placeRepository.findByPublicDataId("BLANK-COORD-1")).isEmpty();
        assertThat(placeRepository.findByPublicDataId("BAD-COORD-1")).isEmpty();

        // 거리순 검색(QueryDSL/PostGIS ST_Distance) 결과에도 좌표 결측 장소가 0,0(Null Island) 등
        // 잘못된 위치로 나타나지 않아야 한다 - 애초에 저장되지 않았으므로 결과에 포함될 수 없다.
        PlaceSearchCondition condition = PlaceSearchCondition.builder()
                .latitude(0.0)
                .longitude(0.0)
                .radiusKm(50.0)
                .build();
        List<PlaceSearchResponseDto> nearNullIsland = placeRepository.searchPlaces(condition);
        assertThat(nearNullIsland).noneMatch(dto ->
                "좌표 빈 문자열 장소".equals(dto.getName()) || "좌표 파싱 불가 장소".equals(dto.getName()));
    }

    @Test
    @DisplayName("contentid가 없는 항목은 저장 대상에서 제외되고, 나머지 정상 항목 처리에는 영향을 주지 않는다")
    void batchWithMissingContentId_SkipsInvalidItem_ProcessesRest() {
        // given
        TourApiPlaceItemDto validItem = TourApiPlaceItemDto.builder()
                .contentId("HAS-ID-1")
                .title("contentId 있는 장소")
                .rawLongitude("126.9")
                .rawLatitude("37.5")
                .build();

        TourApiPlaceItemDto missingIdItem = TourApiPlaceItemDto.builder()
                .contentId(null)
                .title("contentId 없는 장소")
                .rawLongitude("127.0")
                .rawLatitude("37.6")
                .build();

        TourApiPlaceItemDto blankIdItem = TourApiPlaceItemDto.builder()
                .contentId("   ")
                .title("contentId 공백인 장소")
                .rawLongitude("127.1")
                .rawLatitude("37.7")
                .build();

        // when
        int[] result = collectorService.saveCollectedPlaces(List.of(validItem, missingIdItem, blankIdItem));

        // then: contentId가 없거나 공백인 2건은 건너뛰고, 유효한 1건만 저장된다
        assertThat(result[0]).isEqualTo(1);
        assertThat(placeRepository.findByPublicDataId("HAS-ID-1")).isPresent();
        assertThat(placeRepository.findAll())
                .noneMatch(p -> "contentId 없는 장소".equals(p.getName()) || "contentId 공백인 장소".equals(p.getName()));
    }

    private TourApiResponseDto readRealFixture() throws IOException {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("tourapi/success-area-based-list2-real-2026-09-09.json")) {
            assertThat(is).as("실제 areaBasedList2 fixture가 존재해야 한다").isNotNull();
            return objectMapper.readValue(is, TourApiResponseDto.class);
        }
    }
}
