package com.petspot.domain.place.repository;

import com.petspot.domain.place.entity.PublicDataSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공공데이터 동기화 진행 상태 저장소 (TSK-DATA-012)
 */
public interface PublicDataSyncStateRepository extends JpaRepository<PublicDataSyncState, String> {
}
