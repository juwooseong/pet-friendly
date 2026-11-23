package com.petspot.infrastructure.publicdata.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TourPlaceSyncServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    @DisplayName("기준일부터 오늘까지 날짜별로 조회하고(기준일 포함), 기준일이 없거나 31일보다 오래되면 전체 조회 1건")
    void queryPrefixes() {
        assertThat(TourPlaceSyncService.queryPrefixes(LocalDate.of(2026, 11, 19), TODAY))
                .containsExactly("20261119", "20261120", "20261121", "20261122", "20261123");
        assertThat(TourPlaceSyncService.queryPrefixes(TODAY, TODAY)).containsExactly("20261123");

        assertThat(TourPlaceSyncService.queryPrefixes(null, TODAY)).isEqualTo(Collections.singletonList(null));
        assertThat(TourPlaceSyncService.queryPrefixes(TODAY.minusDays(32), TODAY)).isEqualTo(Collections.singletonList(null));
        assertThat(TourPlaceSyncService.queryPrefixes(TODAY.minusDays(31), TODAY)).hasSize(32);
    }
}
