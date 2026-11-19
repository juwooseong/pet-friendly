package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
import com.petspot.infrastructure.publicdata.exception.PublicDataException;
import com.petspot.infrastructure.publicdata.mapper.TourApiPetPolicyParser;
import com.petspot.infrastructure.publicdata.service.TourPetPolicySyncService.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * TSK-DATA-010: 정책 동기화 단계의 처리/중단 규칙 검증 (DB·네트워크 없이).
 */
@ExtendWith(MockitoExtension.class)
class TourPetPolicySyncServiceTest {

    @Mock
    private TourApiClient tourApiClient;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PlatformTransactionManager transactionManager; // TransactionTemplate가 콜백을 그대로 실행하게 한다

    private TourPetPolicySyncService service;

    @BeforeEach
    void setUp() {
        service = new TourPetPolicySyncService(tourApiClient, new TourApiPetPolicyParser(), placeRepository, transactionManager);
    }

    @Test
    @DisplayName("정책이 있으면 해석 결과를 Place에 반영하고, 없으면 '미등록'으로 조회 시각만 남긴다")
    void sync_AppliesPolicyOrMarksNotProvided() {
        Place withPolicy = place("A");
        Place withoutPolicy = place("B");
        givenPending(withPolicy, withoutPolicy);
        given(tourApiClient.fetchPetTourDetail("A")).willReturn(Optional.of(TourApiPetTourItemDto.builder()
                .contentId("A").accompanyType("일부구역 동반가능").allowedPets("10kg 이하 동반 가능").requiredItems("목줄 착용")
                .build()));
        given(tourApiClient.fetchPetTourDetail("B")).willReturn(Optional.empty());

        SyncResult result = service.syncPendingPolicies(10, 0);

        assertThat(result.applied()).isEqualTo(1);
        assertThat(result.notProvided()).isEqualTo(1);
        assertThat(result.stopReason()).isNull();

        assertThat(withPolicy.getMaxWeightLimitKg()).isEqualByComparingTo("10");
        assertThat(withPolicy.getAllowedSizes()).containsExactly("SMALL");
        assertThat(withPolicy.getPetPolicySource()).isEqualTo(Place.POLICY_SOURCE_TOURAPI);
        assertThat(withPolicy.getPetPolicyDetail())
                .containsEntry("accompanyScope", "PARTIAL_AREAS")
                .containsEntry("leashRequired", true)
                .containsEntry("allowedPetsText", "10kg 이하 동반 가능")
                .doesNotContainKey("carrierRequired"); // 근거 없는 항목은 키 자체가 없다(ADR-007)
        assertThat(withPolicy.getPetPolicySyncedAt()).isNotNull();

        assertThat(withoutPolicy.getAllowedSizes()).isNull();
        assertThat(withoutPolicy.getPetPolicySource()).isNull();
        assertThat(withoutPolicy.getPetPolicySyncedAt()).isNotNull(); // 다음 실행에서 다시 조회하지 않음
    }

    @Test
    @DisplayName("일일 호출 한도 초과 오류가 나면 즉시 멈추고 남은 장소는 다음 실행으로 넘긴다")
    void sync_StopsOnQuotaExceeded() {
        Place first = place("A");
        Place second = place("B");
        givenPending(first, second);
        given(tourApiClient.fetchPetTourDetail("A"))
                .willThrow(new PublicDataException("TourAPI 게이트웨이 오류: [22] LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"));

        SyncResult result = service.syncPendingPolicies(10, 0);

        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.stopReason()).contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS");
        verify(tourApiClient, never()).fetchPetTourDetail("B");
        assertThat(first.getPetPolicySyncedAt()).isNull(); // 실패한 장소는 조회 시각을 남기지 않아 재시도 대상
    }

    @Test
    @DisplayName("일시적 오류는 해당 장소만 건너뛰고 계속하되, 연속 5회 실패하면 멈춘다")
    void sync_SkipsTransientErrorsButStopsAfterConsecutiveFailures() {
        Place[] places = new Place[7];
        for (int i = 0; i < places.length; i++) {
            places[i] = place("P" + i);
        }
        givenPending(places);
        given(tourApiClient.fetchPetTourDetail(anyString())).willThrow(new PublicDataException("TourAPI 통신 중 예외가 발생했습니다: timeout"));

        SyncResult result = service.syncPendingPolicies(10, 0);

        assertThat(result.failed()).isEqualTo(TourPetPolicySyncService.MAX_CONSECUTIVE_FAILURES);
        assertThat(result.stopReason()).contains("연속");
        verify(tourApiClient, times(TourPetPolicySyncService.MAX_CONSECUTIVE_FAILURES)).fetchPetTourDetail(anyString());
    }

    @Test
    @DisplayName("관리자가 검증한 정책은 TourAPI 정책으로 덮어쓰지 않는다")
    void sync_DoesNotOverwriteAdminPolicy() {
        Place adminVerified = place("A");
        adminVerified.applyTourApiPetPolicy(null, List.of("SMALL", "MEDIUM", "LARGE"), true, null, ZonedDateTime.now());
        org.springframework.test.util.ReflectionTestUtils.setField(adminVerified, "petPolicySource", Place.POLICY_SOURCE_ADMIN);
        givenPending(adminVerified);
        given(tourApiClient.fetchPetTourDetail("A")).willReturn(Optional.of(TourApiPetTourItemDto.builder()
                .contentId("A").allowedPets("10kg 이하 동반 가능").build()));

        service.syncPendingPolicies(10, 0);

        assertThat(adminVerified.getAllowedSizes()).containsExactly("SMALL", "MEDIUM", "LARGE");
        assertThat(adminVerified.getPetPolicySource()).isEqualTo(Place.POLICY_SOURCE_ADMIN);
    }

    @Test
    @DisplayName("maxCalls가 0 이하이면 아무것도 조회하지 않는다")
    void sync_ZeroCalls() {
        service.syncPendingPolicies(0, 0);
        verify(placeRepository, never()).findByPublicDataIdIsNotNullAndPetPolicySyncedAtIsNullOrderByCreatedAtDesc(any());
    }

    private void givenPending(Place... places) {
        given(placeRepository.findByPublicDataIdIsNotNullAndPetPolicySyncedAtIsNullOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(List.of(places));
        for (Place p : places) {
            org.mockito.Mockito.lenient().when(placeRepository.findById(p.getId())).thenReturn(Optional.of(p));
        }
    }

    private Place place(String contentId) {
        return Place.builder()
                .id(UUID.randomUUID())
                .publicDataId(contentId)
                .name("장소 " + contentId)
                .category("PARK")
                .address("주소")
                .latitude(37.5)
                .longitude(127.0)
                .build();
    }
}
