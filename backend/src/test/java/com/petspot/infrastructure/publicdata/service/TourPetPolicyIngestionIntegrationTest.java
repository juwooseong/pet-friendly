package com.petspot.infrastructure.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService.SyncResult;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * TSK-DATA-010: 장소 목록 저장 → detailPetTour2 정책 조회·해석 → Place 반영 → 크기 필터 검색 노출까지
 * 실제 PostgreSQL/PostGIS(Flyway V9 포함)에서 검증한다. 외부 API는 실제 응답 fixture로 대체한다(네트워크 없음).
 * 테스트 트랜잭션은 끝나면 롤백된다.
 */
@SpringBootTest
@Transactional
class TourPetPolicyIngestionIntegrationTest {

    @MockBean
    private TourApiClient tourApiClient;

    @Autowired
    private TourDataCollectorService collectorService;
    @Autowired
    private TourPetPolicySyncService policySyncService;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("실제 목록·정책 응답으로 적재하면 체중/크기/실내 정책과 상세 정보가 저장되고, 크기 필터 검색에 노출된다")
    void ingestListThenPolicy_PolicyIsSearchable() throws IOException {
        // given: 실제 areaBasedList2 응답(1건) + 실제 detailPetTour2 원문(662268) 문구
        TourApiResponseDto list = readFixture("tourapi/success-area-based-list2-real-2026-09-09.json", TourApiResponseDto.class);
        List<TourApiPlaceItemDto> items = list.getResponse().getBody().getItemList();
        String contentId = items.get(0).getContentId();
        // 테스트 DB에 같은 장소가 남아 있으면 테스트 트랜잭션 안에서 지운다(롤백됨)
        placeRepository.findByPublicDataId(contentId).ifPresent(placeRepository::delete);
        entityManager.flush();
        given(tourApiClient.fetchPetTourDetail(contentId)).willReturn(Optional.of(TourApiPetTourItemDto.builder()
                .contentId(contentId)
                .accompanyType("일부구역 동반가능")
                .allowedPets("15kg 미만 중소형견만 입장 가능(맹견 및 대형견 입장 불가)")
                .requiredItems("반려동물 유모차 탑승,이동장(켄넬)사용")
                .etcInfo("내부 실내 시설 입장 불가 / 야외 좌석 착석은 가능")
                .build()));

        // when: 목록 저장 → 정책 동기화 1건. 테스트 DB에 다른 미조회 장소가 있으면 대상에서 빼 둔다(롤백됨)
        collectorService.saveCollectedPlaces(items);
        entityManager.flush();
        hidePendingPlacesExcept(List.of(contentId));
        SyncResult result = policySyncService.syncPendingPolicies(1, 0);
        entityManager.flush();
        entityManager.clear();

        // then: 정책이 DB에 반영됨
        assertThat(result.applied()).isEqualTo(1);
        Place place = placeRepository.findByPublicDataId(contentId).orElseThrow();
        assertThat(place.getMaxWeightLimitKg()).isEqualByComparingTo("15");
        assertThat(place.getAllowedSizes()).containsExactly("SMALL", "MEDIUM");
        assertThat(place.getIsIndoorAllowed()).isFalse();
        assertThat(place.getPetPolicySource()).isEqualTo(Place.POLICY_SOURCE_TOURAPI);
        assertThat(place.getPetPolicySyncedAt()).isNotNull();
        assertThat(place.getPetPolicyDetail())
                .containsEntry("accompanyScope", "PARTIAL_AREAS")
                .containsEntry("carrierRequired", true)
                .containsEntry("allowedPetsText", "15kg 미만 중소형견만 입장 가능(맹견 및 대형견 입장 불가)");

        // then: 크기 필터 검색 - 소형견 조건에는 나오고 대형견 조건에는 나오지 않는다
        assertThat(searchByNameAndSize(place.getName(), "SMALL")).extracting(PlaceSearchResponseDto::getId).contains(place.getId());
        assertThat(searchByNameAndSize(place.getName(), "LARGE")).extracting(PlaceSearchResponseDto::getId).doesNotContain(place.getId());

        // then: 한 번 조회한 장소는 다음 실행의 대상이 아니다(이어서 처리)
        assertThat(placeRepository.findPendingPetPolicySync(PageRequest.of(0, 100000)))
                .extracting(Place::getId).doesNotContain(place.getId());
    }

    @Test
    @DisplayName("TSK-DATA-011: 정책 조회 대상은 쇼핑이 아닌 장소가 먼저다 - 쇼핑이 더 최근에 등록됐어도")
    void pendingPolicySync_NonShoppingFirst() {
        // given: 관광지 → 쇼핑 → 숙박 순서로 등록 (서해 바다 위 좌표, 실제 장소 없음)
        saveItem("TSK-DATA-011-TOUR", "12");
        saveItem("TSK-DATA-011-SHOP", "38");
        saveItem("TSK-DATA-011-STAY", "32");
        hidePendingPlacesExcept(List.of("TSK-DATA-011-TOUR", "TSK-DATA-011-SHOP", "TSK-DATA-011-STAY"));

        // when
        List<Place> pending = placeRepository.findPendingPetPolicySync(PageRequest.of(0, 10));

        // then: 쇼핑 아닌 장소(최근 등록 순) → 쇼핑
        assertThat(pending).extracting(Place::getPublicDataId)
                .containsExactly("TSK-DATA-011-STAY", "TSK-DATA-011-TOUR", "TSK-DATA-011-SHOP");
        assertThat(pending.get(2).getCategory()).isEqualTo("SHOPPING");
    }

    private void saveItem(String contentId, String contentTypeId) {
        collectorService.saveCollectedPlaces(List.of(TourApiPlaceItemDto.builder()
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .title("정책 우선순위 테스트 " + contentId)
                .address1("테스트 주소")
                .rawLatitude("34.0012")
                .rawLongitude("124.5212")
                .build()));
        entityManager.flush(); // 등록 시각이 저장 순서대로 매겨지도록 하나씩 반영
    }

    /** 다른 테스트가 남긴 장소에 영향받지 않도록, 지정한 장소 외의 미조회 장소를 이 트랜잭션 안에서만 조회 완료로 표시한다. */
    private void hidePendingPlacesExcept(List<String> contentIds) {
        entityManager.createQuery("""
                        update Place p set p.petPolicySyncedAt = :now
                        where p.petPolicySyncedAt is null and p.publicDataId not in :ids
                        """)
                .setParameter("now", ZonedDateTime.now())
                .setParameter("ids", contentIds)
                .executeUpdate();
        entityManager.clear();
    }

    private List<PlaceSearchResponseDto> searchByNameAndSize(String name, String sizeCategory) {
        PlaceSearchCondition condition = new PlaceSearchCondition();
        condition.setKeyword(name);
        condition.setSizeCategory(sizeCategory);
        return placeRepository.searchPlaces(condition);
    }

    private <T> T readFixture(String path, Class<T> type) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new ObjectMapper().readValue(in, type);
        }
    }
}
