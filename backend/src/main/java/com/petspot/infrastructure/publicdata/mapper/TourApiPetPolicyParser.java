package com.petspot.infrastructure.publicdata.mapper;

import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
import com.petspot.infrastructure.publicdata.mapper.ParsedPetPolicy.AccompanyScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI {@code detailPetTour2}의 자유 텍스트를 PetSpot 반려동물 정책 값으로 해석한다.
 *
 * <p>원문은 사람이 작성한 문장이라 형식이 일정하지 않다. 이 파서는 2026-11-19 실제 응답 10건에서 관찰된 표현을
 * 기준으로 보수적으로 해석하며, 확신할 수 없는 항목은 {@code null}(미확인)로 남긴다(ADR-007).
 * 해석하지 못한 표현이 새로 발견되면 실제 원문을 fixture로 추가하고 규칙을 넓힌다.
 *
 * <p>크기 경계는 {@code PetSizeCategory}와 같다: SMALL ≤ 10kg, MEDIUM ≤ 20kg, LARGE &gt; 20kg.
 */
@Component
public class TourApiPetPolicyParser {

    private static final String SMALL = "SMALL";
    private static final String MEDIUM = "MEDIUM";
    private static final String LARGE = "LARGE";
    private static final List<String> ALL_SIZES = List.of(SMALL, MEDIUM, LARGE);

    /** "10kg 이하", "15kg 미만", "7 kg까지", "5㎏" */
    private static final Pattern WEIGHT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:kg|KG|Kg|㎏|킬로)");
    /** "전 견종 동반 가능", "모든 견종", "견종 무관", "크기 제한 없음" */
    private static final Pattern ALL_BREEDS = Pattern.compile("전\\s*견종|모든\\s*견종|견종\\s*(?:무관|제한\\s*없)|크기\\s*(?:무관|제한\\s*없)");
    private static final Pattern SMALL_MEDIUM = Pattern.compile("중\\s*소형|소\\s*중형");
    private static final Pattern SMALL_ONLY = Pattern.compile("소형견");
    private static final Pattern LARGE_DENIED = Pattern.compile("대형견[^,.()/]{0,6}(?:불가|제한|금지)");
    /** "1인 당 최대 2마리", "최대 3마리", "2마리까지" */
    private static final Pattern MAX_COUNT = Pattern.compile("최대\\s*(\\d+)\\s*마리|(\\d+)\\s*마리\\s*(?:까지|이하)");
    private static final Pattern LEASH = Pattern.compile("목줄|리드\\s*줄|하네스");
    private static final Pattern CARRIER = Pattern.compile("이동장|켄넬|케이지|이동\\s*가방|유모차|캐리어");
    private static final Pattern VACCINATION = Pattern.compile("예방\\s*접종|광견병\\s*접종");
    private static final Pattern MUZZLE = Pattern.compile("맹견[^.]{0,20}입마개");
    /** "내부 실내 시설 입장 불가", "실내 동반 불가" */
    private static final Pattern INDOOR_DENIED = Pattern.compile("실내[^/.,]{0,10}불가");
    private static final Pattern INDOOR_ALLOWED = Pattern.compile("실내[^/.,]{0,6}(?:동반|입장|출입)\\s*가능");

    public ParsedPetPolicy parse(TourApiPetTourItemDto item) {
        if (item == null) {
            return ParsedPetPolicy.unknown();
        }

        // 동반 가능 동물이 비어 있으면 같은 문구가 들어 있는 경우가 많은 relaAcdntRiskMtr를 보조로 사용
        String allowedText = firstNonBlank(item.getAllowedPets(), item.getAccidentRiskInfo());
        String requirements = blankToNull(item.getRequiredItems());
        String etc = blankToNull(item.getEtcInfo());
        String scopeText = blankToNull(item.getAccompanyType());

        if (allowedText == null && requirements == null && etc == null && scopeText == null) {
            return ParsedPetPolicy.unknown();
        }

        // 체중: 동반 가능 동물 문구 우선, 없으면 기타 정보에서 찾는다
        BigDecimal maxWeight = findWeight(allowedText);
        if (maxWeight == null) {
            maxWeight = findWeight(etc);
        }

        String all = join(allowedText, requirements, etc);

        return new ParsedPetPolicy(
                parseScope(scopeText),
                maxWeight,
                parseSizes(allowedText, etc, maxWeight),
                parseMaxCount(all),
                matchesOrNull(LEASH, join(requirements, etc)),
                matchesOrNull(CARRIER, join(requirements, etc)),
                matchesOrNull(VACCINATION, all),
                matchesOrNull(MUZZLE, all),
                parseIndoor(all),
                allowedText,
                requirements,
                etc
        );
    }

    private AccompanyScope parseScope(String text) {
        if (text == null) {
            return null;
        }
        String compact = text.replace(" ", "");
        if (compact.contains("전구역")) {
            return AccompanyScope.ALL_AREAS;
        }
        if (compact.contains("일부구역")) {
            return AccompanyScope.PARTIAL_AREAS;
        }
        return null;
    }

    private BigDecimal findWeight(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = WEIGHT.matcher(text);
        return m.find() ? new BigDecimal(m.group(1)) : null;
    }

    /**
     * 우선순위: "전 견종" 명시 → 체중 제한에서 유도 → 크기 키워드. 근거가 없으면 null.
     * "전 견종"과 체중 제한이 함께 있으면(예: "전 견종 가능, 단 20kg 이하") 체중 제한을 우선한다.
     */
    private List<String> parseSizes(String allowedText, String etc, BigDecimal maxWeight) {
        if (maxWeight != null) {
            double w = maxWeight.doubleValue();
            if (w <= 10.0) {
                return List.of(SMALL);
            }
            if (w <= 20.0) {
                return List.of(SMALL, MEDIUM);
            }
            return ALL_SIZES;
        }
        String text = join(allowedText, etc);
        if (text == null) {
            return null;
        }
        if (ALL_BREEDS.matcher(text).find()) {
            return ALL_SIZES;
        }
        if (SMALL_MEDIUM.matcher(text).find() || LARGE_DENIED.matcher(text).find()) {
            return List.of(SMALL, MEDIUM);
        }
        if (SMALL_ONLY.matcher(text).find()) {
            return List.of(SMALL);
        }
        return null;
    }

    private Integer parseMaxCount(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = MAX_COUNT.matcher(text);
        if (!m.find()) {
            return null;
        }
        return Integer.valueOf(m.group(1) != null ? m.group(1) : m.group(2));
    }

    /** 실내 출입은 "불가"를 먼저 본다 - "야외 좌석 가능 / 실내 입장 불가"처럼 함께 쓰이는 경우가 많다. */
    private Boolean parseIndoor(String text) {
        if (text == null) {
            return null;
        }
        if (INDOOR_DENIED.matcher(text).find()) {
            return false;
        }
        if (INDOOR_ALLOWED.matcher(text).find()) {
            return true;
        }
        return null;
    }

    /** 언급이 있으면 true, 없으면 null(미확인) - "언급 없음"을 "필요 없음(false)"으로 해석하지 않는다. */
    private Boolean matchesOrNull(Pattern pattern, String text) {
        return text != null && pattern.matcher(text).find() ? Boolean.TRUE : null;
    }

    private static String firstNonBlank(String a, String b) {
        String first = blankToNull(a);
        return first != null ? first : blankToNull(b);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) {
                if (!sb.isEmpty()) {
                    sb.append(" / ");
                }
                sb.append(p);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
