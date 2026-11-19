package com.petspot.infrastructure.publicdata.mapper;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI 상세 응답에서 반려인에게 유용한 부가 정보를 뽑는다 (홈페이지, 주차, 문의처, 유모차, 이용 요금).
 *
 * <p>{@code detailIntro2}의 필드 이름은 장소 유형마다 접미사가 다르다(예: 주차 {@code parking}, {@code parkingculture},
 * {@code parkingleports}, {@code parkingshopping}, {@code parkinglodging}, {@code parkingfood}). 후보 중 값이 있는 첫 필드를 쓴다.
 * 결과 맵의 키: {@code homepage}, {@code parking}, {@code infoCenter}, {@code babyCarriage}, {@code useFee}. 값이 없는 항목은 키가 없다.
 */
@Component
public class TourApiDetailInfoExtractor {

    public static final String HOMEPAGE = "homepage";
    public static final String PARKING = "parking";
    public static final String INFO_CENTER = "infoCenter";
    public static final String BABY_CARRIAGE = "babyCarriage";
    public static final String USE_FEE = "useFee";

    private static final List<String> SUFFIXES = List.of("", "culture", "leports", "shopping", "lodging", "food");
    /** 홈페이지는 보통 {@code <a href="http://...">...</a>} 형태로 온다. http/https 주소만 허용한다(javascript: 등 차단) */
    private static final Pattern HTTP_URL = Pattern.compile("https?://[^\\s\"'<>]+");

    public Map<String, String> extract(String rawHomepage, Map<String, String> introFields) {
        Map<String, String> info = new LinkedHashMap<>();
        putIfPresent(info, HOMEPAGE, extractUrl(rawHomepage));
        if (introFields != null) {
            putIfPresent(info, PARKING, firstPresent(introFields, "parking"));
            putIfPresent(info, INFO_CENTER, firstPresent(introFields, "infocenter"));
            putIfPresent(info, BABY_CARRIAGE, firstPresent(introFields, "chkbabycarriage"));
            putIfPresent(info, USE_FEE, firstPresent(introFields, "usefee"));
        }
        return info.isEmpty() ? null : info;
    }

    /** HTML 조각에서 첫 http(s) 주소를 꺼낸다. 없으면 null */
    static String extractUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher m = HTTP_URL.matcher(raw.replace("&amp;", "&"));
        return m.find() ? m.group() : null;
    }

    private String firstPresent(Map<String, String> fields, String baseKey) {
        for (String suffix : SUFFIXES) {
            String value = TourApiText.clean(fields.get(baseKey + suffix));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
