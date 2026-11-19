package com.petspot.infrastructure.publicdata.mapper;

import com.petspot.domain.place.entity.Place;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;


/**
 * TourAPI DTO -> Place Entity 변환 전용 매퍼 컴포넌트
 * (Entity 생성 및 PostGIS Point 변환 책임)
 */
@Component
public class TourApiPlaceMapper {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private static final String DEFAULT_CATEGORY = "ETC";
    private static final String DEFAULT_CATEGORY_NAME = "기타 펫동반 장소";

    /**
     * TourApiPlaceItemDto 객체를 Place 엔티티로 매핑 변환
     *
     * <p><b>TSK-DATA-003-B 좌표 결측 정책(ADR-008):</b> {@code mapx}/{@code mapy}가 빈 문자열이거나 숫자로
     * 파싱할 수 없는 경우 {@code null}을 반환한다(전략 B — 좌표 없는 장소는 적재하지 않음). {@code Place}의
     * {@code latitude}/{@code longitude}/{@code location} 컬럼이 DB {@code NOT NULL}이라 실제 좌표 없이는
     * 유효한 Entity를 만들 수 없으므로, 과거처럼 {@code 0.0}/{@code 0.0}(Null Island)으로 임의 대체하지
     * 않는다 — 실제 위치가 없는 장소를 유효한 좌표를 가진 장소처럼 지도/거리 검색에 노출시키는 것을 막기
     * 위함이다.
     *
     * @param dto TourAPI 단일 항목 DTO
     * @return 매핑된 Place 엔티티, 또는 {@code dto}가 {@code null}이거나 좌표를 파싱할 수 없으면 {@code null}
     */
    public Place toEntity(TourApiPlaceItemDto dto) {
        if (dto == null) {
            return null;
        }

        Double longitude = dto.getLongitude();
        Double latitude = dto.getLatitude();
        if (longitude == null || latitude == null) {
            return null;
        }

        // PostGIS SRID 4326 Point 객체 생성 (x: lon, y: lat)
        Point locationPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        String category = mapCategory(dto.getContentTypeId(), dto.getCategory3());
        String categoryName = dto.getCategory3() != null ? dto.getCategory3() : DEFAULT_CATEGORY_NAME;

        return Place.builder()
                .publicDataId(dto.getContentId())
                .name(dto.getTitle() != null ? dto.getTitle() : "미지정 장소")
                .category(category)
                .categoryName(categoryName)
                .address(dto.getFullAddress())
                .location(locationPoint)
                .latitude(latitude)
                .longitude(longitude)
                .phone(dto.getTel())
                .imageUrl(dto.getFirstImage() != null ? dto.getFirstImage() : dto.getFirstImageThumbnail())
                // 반려동물 정책(maxWeightLimitKg/allowedSizes/실내외 등)은 목록 응답(areaBasedList2)에 없다.
                // 장소별 detailPetTour2 응답을 TourApiPetPolicyParser로 해석해 별도로 반영한다(ADR-009).
                // 이 단계에서는 근거 없이 채우지 않고 null(미확인)로 둔다(ADR-007).
                .build();
    }

    /**
     * TourAPI contentTypeId / cat3 기준 자사 카테고리 매핑
     */
    private String mapCategory(String contentTypeId, String cat3) {
        if ("39".equals(contentTypeId)) {
            return "CAFE"; // 음식점/카페
        } else if ("32".equals(contentTypeId)) {
            return "HOTEL"; // 숙박
        } else if ("12".equals(contentTypeId) || "28".equals(contentTypeId)) {
            return "PARK"; // 관광지/레포츠/공원
        }
        return DEFAULT_CATEGORY;
    }
}
