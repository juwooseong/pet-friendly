package com.petspot.infrastructure.publicdata.client;

import com.petspot.infrastructure.publicdata.dto.TourApiResponseDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TSK-DATA-002: 실제 data.go.kr TourAPI(KorPetTourService2)를 호출하는 통합 테스트.
 *
 * 기본 {@code ./gradlew test} 실행에서는 항상 스킵된다. 실행하려면 로컬 프로필 시크릿(application-local.yml,
 * gitignored)이 채워진 상태에서 아래처럼 시스템 프로퍼티를 지정해야 한다:
 *   ./gradlew test --tests "*TourApiClientRealApiIntegrationTest" -Dspring.profiles.active=local -DrunTourApiIntegrationTest=true
 *
 * 이 프로젝트에는 별도의 통합테스트 소스셋/태그 분리 관례가 없어(전부 src/test/java 단일 소스셋),
 * JUnit5 @EnabledIfSystemProperty로 옵트인 게이트만 최소 적용한다.
 */
@Tag("integration")
class TourApiClientRealApiIntegrationTest {

    @Test
    @EnabledIfSystemProperty(named = "runTourApiIntegrationTest", matches = "true")
    void fetchPetPlaces_RealApiCall() {
        String serviceKey = System.getProperty("tourApiServiceKey", System.getenv("TOUR_API_SERVICE_KEY"));
        assertThat(serviceKey).as("실제 호출을 위해서는 tourApiServiceKey 시스템 프로퍼티 또는 TOUR_API_SERVICE_KEY 환경변수가 필요합니다.")
                .isNotBlank();

        TourApiClient client = new TourApiClient(
                RestClient.builder().build(),
                serviceKey,
                "https://apis.data.go.kr/B551011/KorPetTourService2"
        );

        // TSK-DATA-002 조사 시점 기준 이 키는 SERVICE_KEY_IS_NOT_REGISTERED_ERROR(reasonCode 30)를 반환했다.
        // 키가 승인된 이후에는 이 테스트가 실제 목록을 반환하는지로 재검증한다.
        TourApiResponseDto response = client.fetchPetPlaces(5, 1);

        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getHeader().getResultCode()).isEqualTo("0000");
    }
}
