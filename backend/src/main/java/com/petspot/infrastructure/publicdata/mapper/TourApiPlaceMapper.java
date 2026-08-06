package com.petspot.infrastructure.publicdata.mapper;

import com.petspot.domain.place.entity.Place;
import com.petspot.infrastructure.publicdata.dto.TourApiPlaceItemDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI DTO -> Place Entity 변환 전용 매퍼 컴포넌트
 * (Entity 생성 및 PostGIS Point 변환 책임)
 */
@Component
public class TourApiPlaceMapper {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*kg", Pattern.CASE_INSENSITIVE);

    private static final String DEFAULT_CATEGORY = "ETC";
    private static final String DEFAULT_CATEGORY_NAME = "기타 펫동반 장소";

    /**
     * TourApiPlaceItemDto 객체를 Place 엔티티로 매핑 변환
     *
     * @param dto TourAPI 단일 항목 DTO
     * @return Place 엔티티
     */
    public Place toEntity(TourApiPlaceItemDto dto) {
        if (dto == null) {
            return null;
        }

        Double longitude = dto.getLongitude() != null ? dto.getLongitude() : 0.0;
        Double latitude = dto.getLatitude() != null ? dto.getLatitude() : 0.0;

        // PostGIS SRID 4326 Point 객체 생성 (x: lon, y: lat)
        Point locationPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        String category = mapCategory(dto.getContentTypeId(), dto.getCategory3());
        String categoryName = dto.getCategory3() != null ? dto.getCategory3() : DEFAULT_CATEGORY_NAME;
        Double maxWeight = extractWeightLimit(dto.getPetWeightLimitText());

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
                .description(dto.getPetPolicyInfo())
                .maxWeightLimitKg(maxWeight != null ? java.math.BigDecimal.valueOf(maxWeight) : null)
                .isIndoorAllowed(true)
                .isOutdoorAllowed(true)
                .hasOffLeashZone(false)
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

    /**
     * "10kg 이하 가능" 과 같은 동반 조건 텍스트에서 숫자 체중(kg) 추출
     */
    private Double extractWeightLimit(String weightText) {
        if (weightText == null || weightText.isBlank()) {
            return null;
        }
        Matcher matcher = WEIGHT_PATTERN.matcher(weightText);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
