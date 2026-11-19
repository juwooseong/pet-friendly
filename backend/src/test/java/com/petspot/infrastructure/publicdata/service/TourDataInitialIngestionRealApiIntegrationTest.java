package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TSK-DATA-003-A: 실제 data.go.kr TourAPI(KorPetTourService2/areaBasedList2)를 호출해 최대 5건을
 * 실제 로컬 PostgreSQL/PostGIS Place 테이블에 저장하는 초기 적재 최소 파이프라인 통합 테스트.
 *
 * <p>기본 {@code ./gradlew test} 실행에서는 항상 스킵된다({@code TourApiClientRealApiIntegrationTest}와
 * 동일한 opt-in 게이팅 패턴). 실행하려면 로컬 프로필 시크릿(application-local.yml, gitignored)이 채워진
 * 상태에서 아래처럼 시스템 프로퍼티를 지정해야 한다:
 *   ./gradlew test --tests "*TourDataInitialIngestionRealApiIntegrationTest" -Dspring.profiles.active=local -DrunTourApiIntegrationTest=true
 *
 * <p>절대 전국/대량 데이터를 적재하지 않는다 - {@code numOfRows=5, pageNo=1} 단 1회 호출만 수행한다.
 * Spring이 관리하는 실제 {@link TourDataCollectorService}/{@link PlaceRepository} 빈을 그대로 사용해
 * 실제 로컬 DB에 저장까지 검증하되, {@code @Transactional}로 테스트 종료 시 자동 롤백되어 실행 후 DB에
 * 데이터가 남지 않는다. serviceKey는 로그/코드/assertion 메시지 어디에도 직접 출력하지 않는다.
 */
@SpringBootTest
@Transactional
@Tag("integration")
class TourDataInitialIngestionRealApiIntegrationTest {

    @Autowired
    private TourDataCollectorService collectorService;

    @Autowired
    private PlaceRepository placeRepository;

    @Test
    @DisplayName("실제 areaBasedList2(최대 5건)를 1회 호출해 DB에 저장하고, 기본정보는 채워지고 반려동물 정책 5개 필드는 전부 null임을 검증한다")
    @EnabledIfSystemProperty(named = "runTourApiIntegrationTest", matches = "true")
    void ingestFiveRealPlaces_SavesToDatabase_WithNullPolicyFields() {
        // when: 최대 5건만 실제 호출 (1회)
        List<TourApiPlaceItemDto> items = collectorService.collectPage(5, 1);
        assertThat(items).as("실제 areaBasedList2 응답에 최소 1건이 반환되어야 한다").isNotEmpty();
        assertThat(items.size()).isLessThanOrEqualTo(5);

        int[] result = collectorService.saveCollectedPlaces(items);
        // TSK-DATA-003-B(ADR-008, 전략 B): contentId 결측 또는 좌표 결측 항목은 저장에서 제외되므로
        // saved+updated 합계가 items.size()보다 작을 수 있다(스킵된 항목 존재 시).
        assertThat(result[0] + result[1]).isLessThanOrEqualTo(items.size());

        // then: 저장된 각 장소의 기본정보/정책 필드 검증 (contentId 또는 좌표가 없어 스킵된 항목은 제외)
        for (TourApiPlaceItemDto dto : items) {
            if (dto.getContentId() == null || dto.getContentId().isBlank()) {
                continue;
            }
            if (dto.getLongitude() == null || dto.getLatitude() == null) {
                continue;
            }
            Optional<Place> savedOpt = placeRepository.findByPublicDataId(dto.getContentId());
            assertThat(savedOpt).as("contentId=" + dto.getContentId() + " 장소가 저장되어야 한다").isPresent();
            Place saved = savedOpt.get();

            assertThat(saved.getPublicDataId()).isEqualTo(dto.getContentId());
            assertThat(saved.getName()).isNotBlank();
            assertThat(saved.getLatitude()).isNotNull();
            assertThat(saved.getLongitude()).isNotNull();

            // 반려동물 정책 5개 필드는 TourAPI 근거가 없으므로 전부 null이어야 한다(TSK-DATA-008 원칙)
            assertThat(saved.getMaxWeightLimitKg()).isNull();
            assertThat(saved.getAllowedSizes()).isNull();
            assertThat(saved.getIsIndoorAllowed()).isNull();
            assertThat(saved.getIsOutdoorAllowed()).isNull();
            assertThat(saved.getHasOffLeashZone()).isNull();
        }
    }
}
