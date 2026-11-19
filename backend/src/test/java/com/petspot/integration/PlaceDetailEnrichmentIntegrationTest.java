package com.petspot.integration;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiDetailCommonDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 장소 상세 조회 시 TourAPI 상세 정보(소개/운영시간) 지연 적재 검증 - 실제 DB, 외부 API는 실제 응답 값으로 대체.
 *
 * <p>지연 적재는 별도(REQUIRES_NEW) 트랜잭션으로 커밋되므로 테스트 트랜잭션 롤백에 의존하지 않고 직접 정리한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlaceDetailEnrichmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PlaceRepository placeRepository;
    @MockBean
    private TourApiClient tourApiClient;

    private UUID placeId;

    @BeforeEach
    void setUp() {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Place place = placeRepository.save(Place.builder()
                .publicDataId("TEST-ENRICH-" + UUID.randomUUID())
                .name("상세 지연 적재 테스트 약국")
                .category("ETC")
                .address("서울시 테스트구")
                .location(gf.createPoint(new Coordinate(127.0, 37.5)))
                .latitude(37.5)
                .longitude(127.0)
                .build());
        placeId = place.getId();
    }

    @AfterEach
    void tearDown() {
        placeRepository.deleteById(placeId);
    }

    @Test
    @DisplayName("첫 상세 조회 때 TourAPI에서 소개·운영시간을 가져와 응답에 포함하고, 두 번째 조회부터는 다시 호출하지 않는다")
    void firstView_FetchesAndStores_SecondViewUsesDb() throws Exception {
        String contentId = placeRepository.findById(placeId).orElseThrow().getPublicDataId();
        given(tourApiClient.fetchDetailCommon(contentId)).willReturn(Optional.of(TourApiDetailCommonDto.builder()
                .contentId(contentId).contentTypeId("38")
                .overview("가까운약국은 이웃처럼 편안한 상담과<br>정직한 약료 서비스를 지향합니다.")
                .build()));
        given(tourApiClient.fetchDetailIntro(contentId, "38"))
                .willReturn(Optional.of(Map.of("opentime", "09:00-20:00", "restdateshopping", "")));

        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description", is("가까운약국은 이웃처럼 편안한 상담과\n정직한 약료 서비스를 지향합니다.")))
                .andExpect(jsonPath("$.data.operatingHours", is("이용 시간: 09:00-20:00")));

        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description", is("가까운약국은 이웃처럼 편안한 상담과\n정직한 약료 서비스를 지향합니다.")));

        verify(tourApiClient, times(1)).fetchDetailCommon(contentId);
        verify(tourApiClient, times(1)).fetchDetailIntro(contentId, "38");
    }

    @Test
    @DisplayName("TourAPI 오류가 나도 상세 조회는 정상 응답하고, 다음 조회 때 다시 시도한다")
    void apiFailure_DetailStillWorks_AndRetriesNextTime() throws Exception {
        given(tourApiClient.fetchDetailCommon(anyString()))
                .willThrow(new PublicDataException("TourAPI 게이트웨이 오류: [22] LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"));

        mockMvc.perform(get("/api/v1/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("상세 지연 적재 테스트 약국")))
                .andExpect(jsonPath("$.data.description", nullValue()));
        mockMvc.perform(get("/api/v1/places/" + placeId)).andExpect(status().isOk());

        verify(tourApiClient, times(2)).fetchDetailCommon(anyString());
        verify(tourApiClient, never()).fetchDetailIntro(anyString(), anyString());
    }
}
