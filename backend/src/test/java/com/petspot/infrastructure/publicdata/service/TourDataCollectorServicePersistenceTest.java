package com.petspot.infrastructure.publicdata.service;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.place.repository.PlaceRepository;
import com.petspot.infrastructure.publicdata.client.TourApiClient;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import com.petspot.infrastructure.publicdata.mapper.TourApiPlaceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TourDataCollectorServicePersistenceTest {

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourApiPlaceMapper tourApiPlaceMapper;

    @Mock
    private PlaceRepository placeRepository;

    private TourDataCollectorService collectorService;

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        collectorService = new TourDataCollectorService(tourApiClient, tourApiPlaceMapper, placeRepository);
    }

    @Test
    @DisplayName("신규 데이터 저장 및 기존 중복 데이터 정보 업데이트 동작 검증")
    void saveCollectedPlaces_NewAndDuplicate() {
        // given
        TourApiPlaceItemDto dtoNew = TourApiPlaceItemDto.builder().contentId("NEW-1").title("신규 장소").build();
        TourApiPlaceItemDto dtoDup = TourApiPlaceItemDto.builder().contentId("DUP-1").title("중복 장소").build();

        Place entityNew = Place.builder()
                .publicDataId("NEW-1")
                .name("신규 장소")
                .address("주소1")
                .location(geometryFactory.createPoint(new Coordinate(127.0, 37.0)))
                .latitude(37.0)
                .longitude(127.0)
                .build();

        Place entityDupExisting = Place.builder()
                .publicDataId("DUP-1")
                .name("기존 장소 이름")
                .address("기존 주소")
                .location(geometryFactory.createPoint(new Coordinate(127.1, 37.1)))
                .latitude(37.1)
                .longitude(127.1)
                .build();

        Place entityDupTemp = Place.builder()
                .publicDataId("DUP-1")
                .name("업데이트된 장소 이름")
                .address("기존 주소")
                .location(geometryFactory.createPoint(new Coordinate(127.1, 37.1)))
                .latitude(37.1)
                .longitude(127.1)
                .build();

        given(placeRepository.findByPublicDataId("NEW-1")).willReturn(Optional.empty());
        given(placeRepository.findByPublicDataId("DUP-1")).willReturn(Optional.of(entityDupExisting));
        given(tourApiPlaceMapper.toEntity(dtoNew)).willReturn(entityNew);
        given(tourApiPlaceMapper.toEntity(dtoDup)).willReturn(entityDupTemp);

        // when
        int[] result = collectorService.saveCollectedPlaces(List.of(dtoNew, dtoDup));

        // then
        assertThat(result[0]).isEqualTo(1); // 신규 저장 1건
        assertThat(result[1]).isEqualTo(1); // 중복 업데이트 1건

        verify(placeRepository).save(entityNew);
        assertThat(entityDupExisting.getName()).isEqualTo("업데이트된 장소 이름");
    }
}
