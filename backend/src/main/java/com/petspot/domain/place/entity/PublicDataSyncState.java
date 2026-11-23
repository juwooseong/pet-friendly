package com.petspot.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 공공데이터 동기화 진행 상태 (TSK-DATA-012). 마지막으로 빠짐없이 반영한 날짜를 저장한다.
 */
@Entity
@Table(name = "public_data_sync_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicDataSyncState {

    /** TourAPI 반려동물 동반 장소 목록 동기화 */
    public static final String TOUR_PET_PLACES = "TOUR_PET_PLACES";

    @Id
    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "last_synced_date", nullable = false)
    private LocalDate lastSyncedDate;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public PublicDataSyncState(String name, LocalDate lastSyncedDate) {
        this.name = name;
        this.lastSyncedDate = lastSyncedDate;
        this.updatedAt = ZonedDateTime.now();
    }

    public void advanceTo(LocalDate date) {
        this.lastSyncedDate = date;
        this.updatedAt = ZonedDateTime.now();
    }
}
