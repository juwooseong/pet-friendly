package com.petspot.domain.place.repository;

import com.petspot.domain.place.dto.PlaceSearchCondition;
import com.petspot.domain.place.dto.PlaceSearchResponseDto;
import com.petspot.domain.place.dto.QPlaceSearchResponseDto;
import com.petspot.domain.place.entity.QPlace;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QueryDSL + PostGIS 기반 커스텀 Place Repository 구현체
 * PostGIS Spatial Index (GIST) 및 ST_DWithin() 기반 반경 거리 검색 최적화
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceRepositoryCustom {

    private static final int SRID_WGS84 = 4326;
    private static final double METERS_PER_KM = 1000.0;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition) {
        return searchPlaces(condition, Pageable.unpaged()).getContent();
    }

    @Override
    public Page<PlaceSearchResponseDto> searchPlaces(PlaceSearchCondition condition, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        log.debug("Executing QueryDSL place search with ST_DWithin & Pageable: lat={}, lon={}, radiusKm={}, page={}, size={}",
                condition.getLatitude(), condition.getLongitude(), condition.getRadiusKm(),
                pageable.isPaged() ? pageable.getPageNumber() : 0,
                pageable.isPaged() ? pageable.getPageSize() : "unpaged");

        QPlace place = QPlace.place;

        // 1. 위경도 기준 거리 계산 표현식 (ST_Distance geography 기반 거리(km) 계산)
        NumberTemplate<Double> distanceExpression = null;
        if (condition.getLatitude() != null && condition.getLongitude() != null) {
            distanceExpression = Expressions.numberTemplate(Double.class,
                    "ST_Distance(geography({0}), geography(ST_SetSRID(ST_MakePoint({1}, {2}), 4326))) / 1000.0",
                    place.location, condition.getLongitude(), condition.getLatitude());
        }

        BooleanExpression[] whereConditions = new BooleanExpression[]{
                stDWithin(condition.getLatitude(), condition.getLongitude(), condition.getRadiusKmOrDefault()),
                categoryEq(condition.getCategory()),
                keywordContains(condition.getKeyword()),
                maxWeightGte(condition.getMaxWeight()),
                sizeCategoryAllowed(condition.getSizeCategory())
        };

        var query = queryFactory
                .select(new QPlaceSearchResponseDto(
                        place.id,
                        place.publicDataId,
                        place.name,
                        place.category,
                        place.categoryName,
                        place.address,
                        place.latitude,
                        place.longitude,
                        place.phone,
                        place.operatingHours,
                        place.imageUrl,
                        place.rating,
                        place.reviewCount,
                        place.maxWeightLimitKg,
                        place.allowedSizes,
                        distanceExpression != null ? distanceExpression : Expressions.nullExpression(Double.class)
                ))
                .from(place)
                .where(whereConditions)
                .orderBy(
                        distanceExpression != null ? distanceExpression.asc() : place.rating.desc(),
                        place.reviewCount.desc()
                );

        if (pageable.isPaged()) {
            query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        }

        List<PlaceSearchResponseDto> content = query.fetch();

        // 2. Total Count 쿼리
        Long total = queryFactory
                .select(place.count())
                .from(place)
                .where(whereConditions)
                .fetchOne();

        long totalCount = total != null ? total : 0L;
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("[QUERYDSL PAGE SUCCESS] Search completed. Total: {}, Time: {} ms", totalCount, elapsedTime);

        return new org.springframework.data.domain.PageImpl<>(content, pageable.isPaged() ? pageable : Pageable.unpaged(), totalCount);
    }

    @Override
    public Optional<PlaceSearchResponseDto> findPlaceDetail(UUID placeId) {
        QPlace place = QPlace.place;

        PlaceSearchResponseDto result = queryFactory
                .select(new QPlaceSearchResponseDto(
                        place.id,
                        place.publicDataId,
                        place.name,
                        place.category,
                        place.categoryName,
                        place.address,
                        place.latitude,
                        place.longitude,
                        place.phone,
                        place.operatingHours,
                        place.imageUrl,
                        place.rating,
                        place.reviewCount,
                        place.maxWeightLimitKg,
                        place.allowedSizes,
                        Expressions.nullExpression(Double.class)
                ))
                .from(place)
                .where(place.id.eq(placeId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * PostGIS ST_DWithin() 기반 공간 반경 필터 (PostgreSQL GIST 공간 인덱스 사용)
     * ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography, radiusMeters)
     */
    private BooleanExpression stDWithin(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null || radiusKm == null) {
            return null;
        }
        double radiusMeters = radiusKm * METERS_PER_KM;
        return Expressions.booleanTemplate(
                "cast(ST_DWithin(geography({0}), geography(ST_SetSRID(ST_MakePoint({1}, {2}), 4326)), {3}) as boolean) = true",
                QPlace.place.location, longitude, latitude, radiusMeters
        );
    }

    /**
     * 카테고리 일치 조건 (CAFE, HOTEL, PARK, HOSPITAL, SALON)
     */
    private BooleanExpression categoryEq(String category) {
        return (category != null && !category.isBlank()) ? QPlace.place.category.eq(category) : null;
    }

    /**
     * 장소명 또는 주소 키워드 검색 조건 (LIKE %keyword%)
     */
    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return QPlace.place.name.containsIgnoreCase(keyword)
                .or(QPlace.place.address.containsIgnoreCase(keyword));
    }

    /**
     * 동반 가능 최대 체중 조건 (maxWeightLimitKg IS NULL OR maxWeightLimitKg >= 펫체중)
     */
    private BooleanExpression maxWeightGte(Double petWeight) {
        if (petWeight == null) {
            return null;
        }
        return QPlace.place.maxWeightLimitKg.isNull()
                .or(QPlace.place.maxWeightLimitKg.goe(petWeight));
    }

    /**
     * 반려동물 크기 조건 (allowed_sizes JSONB 배열에 sizeCategory 문자열이 포함되어야 함)
     * PostgreSQL jsonb_exists(jsonb, text) 함수를 사용해 배열 내 최상위 요소 존재 여부를 확인한다.
     */
    private BooleanExpression sizeCategoryAllowed(String sizeCategory) {
        if (sizeCategory == null || sizeCategory.isBlank()) {
            return null;
        }
        return Expressions.booleanTemplate(
                "cast(jsonb_exists({0}, {1}) as boolean) = true",
                QPlace.place.allowedSizes, sizeCategory
        );
    }
}
