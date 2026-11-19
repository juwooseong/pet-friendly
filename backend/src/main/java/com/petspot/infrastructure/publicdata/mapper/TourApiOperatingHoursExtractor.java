package com.petspot.infrastructure.publicdata.mapper;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TourAPI {@code detailIntro2} 응답에서 운영시간 안내 문구를 만든다.
 *
 * <p>필드 이름이 장소 유형(contentTypeId)마다 다르다(TourAPI 4.0 공통 스키마 기준, 쇼핑/숙박은 실제 응답으로 확인):
 * <ul>
 *   <li>이용 시간: 관광지 {@code usetime}, 문화시설 {@code usetimeculture}, 레포츠 {@code usetimeleports},
 *       쇼핑 {@code opentime}, 음식점 {@code opentimefood}, 행사 {@code playtime}</li>
 *   <li>쉬는 날: {@code restdate}, {@code restdateculture}, {@code restdateleports}, {@code restdateshopping}, {@code restdatefood}</li>
 *   <li>숙박: {@code checkintime}, {@code checkouttime}</li>
 * </ul>
 * 유형을 따로 분기하지 않고 후보 필드 중 값이 있는 것을 순서대로 사용한다. 아무 값도 없으면 null.
 */
@Component
public class TourApiOperatingHoursExtractor {

    private static final List<String> USE_TIME_KEYS =
            List.of("usetime", "usetimeculture", "usetimeleports", "opentime", "opentimefood", "playtime");
    private static final List<String> REST_DAY_KEYS =
            List.of("restdate", "restdateculture", "restdateleports", "restdateshopping", "restdatefood");

    public String extract(Map<String, String> introFields) {
        if (introFields == null || introFields.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();

        String useTime = firstPresent(introFields, USE_TIME_KEYS);
        if (useTime != null) {
            lines.add("이용 시간: " + useTime);
        }
        String checkIn = TourApiText.clean(introFields.get("checkintime"));
        String checkOut = TourApiText.clean(introFields.get("checkouttime"));
        if (checkIn != null || checkOut != null) {
            lines.add("체크인 " + (checkIn != null ? checkIn : "-") + " / 체크아웃 " + (checkOut != null ? checkOut : "-"));
        }
        String restDay = firstPresent(introFields, REST_DAY_KEYS);
        if (restDay != null) {
            lines.add("쉬는 날: " + restDay);
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private String firstPresent(Map<String, String> fields, List<String> keys) {
        for (String key : keys) {
            String value = TourApiText.clean(fields.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
