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

        String category = mapCategory(dto.getContentTypeId());
        String categoryName = mapCategoryName(dto.getContentTypeId());

        Place place = Place.builder()
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
        applyListInfo(place, dto);
        return place;
    }

    /**
     * 목록 응답의 분류·이미지 메타 정보를 장소에 반영한다(신규/기존 공통).
     * 이 메서드를 통해서만 반영해야 신규 적재와 재적재 결과가 같아진다.
     */
    public void applyListInfo(Place place, TourApiPlaceItemDto dto) {
        place.applyTourApiListInfo(
                mapCategory(dto.getContentTypeId()),
                mapCategoryName(dto.getContentTypeId()),
                blankToNull(dto.getContentTypeId()),
                blankToNull(dto.getFirstImageThumbnail()),
                blankToNull(dto.getImageCopyrightType()));
    }

    /**
     * TourAPI 장소 유형(contentTypeId) -> PetSpot 카테고리.
     *
     * <p>2026-11-19 전국 적재 결과 9,630곳 중 8,663곳이 ETC였다(쇼핑 38·문화시설 14 등이 매핑되지 않음).
     * 장소 유형 8종을 모두 매핑한다. 12 관광지·28 레포츠는 기존과 같이 PARK, 39 음식점은 CAFE로 유지한다(기존 데이터·화면 호환).
     */
    String mapCategory(String contentTypeId) {
        if (contentTypeId == null) {
            return DEFAULT_CATEGORY;
        }
        return switch (contentTypeId.trim()) {
            case "12", "28" -> "PARK";
            case "14" -> "CULTURE";
            case "15" -> "FESTIVAL";
            case "25" -> "COURSE";
            case "32" -> "HOTEL";
            case "38" -> "SHOPPING";
            case "39" -> "CAFE";
            default -> DEFAULT_CATEGORY;
        };
    }

    /**
     * 화면에 보여줄 분류 이름. 예전에는 cat3 값을 그대로 썼는데, cat3는 "A04010600" 같은 코드이거나 비어 있어
     * (8,662곳이 빈 문자열) 사람이 읽을 수 없었다. 장소 유형 이름을 쓴다.
     */
    String mapCategoryName(String contentTypeId) {
        if (contentTypeId == null) {
            return DEFAULT_CATEGORY_NAME;
        }
        return switch (contentTypeId.trim()) {
            case "12" -> "관광지";
            case "14" -> "문화시설";
            case "15" -> "축제·공연·행사";
            case "25" -> "여행코스";
            case "28" -> "레포츠";
            case "32" -> "숙박";
            case "38" -> "쇼핑";
            case "39" -> "음식점·카페";
            default -> DEFAULT_CATEGORY_NAME;
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
