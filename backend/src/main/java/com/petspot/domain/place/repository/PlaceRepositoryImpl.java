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
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * QueryDSL + PostGIS 기반 커스텀 Place Repository 구현체
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
        long startTime = System.currentTimeMillis();
        log.debug("Executing QueryDSL place search with condition: lat={}, lon={}, radiusKm={}, category={}, keyword={}",
                condition.getLatitude(), condition.getLongitude(), condition.getRadiusKm(),
                condition.getCategory(), condition.getKeyword());

        QPlace place = QPlace.place;

        // 위경도 기반 Haversine 거리 계산 식 (PostGIS 함수 및 일반 SQL 공용 호환)
        NumberTemplate<Double> distanceExpression = null;
        if (condition.getLatitude() != null && condition.getLongitude() != null) {
            distanceExpression = Expressions.numberTemplate(Double.class,
                    "(6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1}))))",
                    condition.getLatitude(), place.latitude, place.longitude, condition.getLongitude());
        }

        List<PlaceSearchResponseDto> results = queryFactory
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
                        distanceExpression != null ? distanceExpression : Expressions.asNumber((Double) null)
                ))
                .from(place)
                .where(
                        radiusWithin(distanceExpression, condition.getRadiusKmOrDefault()),
                        categoryEq(condition.getCategory()),
                        keywordContains(condition.getKeyword()),
                        maxWeightGte(condition.getMaxWeight())
                )
                .orderBy(
                        distanceExpression != null ? distanceExpression.asc() : place.rating.desc(),
                        place.reviewCount.desc()
                )
                .fetch();

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("[QUERYDSL SUCCESS] Search completed. Count: {}, Execution Time: {} ms", results.size(), elapsedTime);

        return results != null ? results : Collections.emptyList();
    }

    /**
     * 반경 거리 이내 검색 조건 (거리 <= radiusKm)
     */
    private BooleanExpression radiusWithin(NumberTemplate<Double> distanceExpr, Double radiusKm) {
        if (distanceExpr == null || radiusKm == null) {
            return null;
        }
        return distanceExpr.loe(radiusKm);
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
}
