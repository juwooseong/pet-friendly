package com.petspot.infrastructure.publicdata.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.infrastructure.publicdata.dto.TourApiPetTourItemDto;
import com.petspot.infrastructure.publicdata.mapper.ParsedPetPolicy.AccompanyScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TSK-DATA-009: detailPetTour2 자유 텍스트 해석 검증.
 *
 * <p>2026-11-19 실제 응답 10건({@code success-detail-pet-tour2-samples-real-2026-11-19.json})을 그대로 입력으로 사용해
 * 실제 표현에 대한 해석 결과를 고정한다. 새 표현이 발견되면 fixture에 원문을 추가하고 이 표에 기대값을 추가한다.
 */
class TourApiPetPolicyParserTest {

    private static final Map<String, TourApiPetTourItemDto> REAL_SAMPLES = new HashMap<>();
    private final TourApiPetPolicyParser parser = new TourApiPetPolicyParser();

    @BeforeAll
    static void loadRealSamples() throws IOException {
        ObjectMapper om = new ObjectMapper();
        try (InputStream in = TourApiPetPolicyParserTest.class
                .getResourceAsStream("/tourapi/success-detail-pet-tour2-samples-real-2026-11-19.json")) {
            JsonNode root = om.readTree(in);
            for (JsonNode node : root.get("items")) {
                TourApiPetTourItemDto dto = om.treeToValue(node, TourApiPetTourItemDto.class);
                REAL_SAMPLES.put(dto.getContentId(), dto);
            }
        }
        assertThat(REAL_SAMPLES).hasSize(10);
    }

    /**
     * 실제 원문별 기대 해석. 빈 칸은 null(미확인).
     * 컬럼: contentId, 동반구역, 최대체중, 허용크기(|구분), 최대마릿수, 목줄, 이동장, 예방접종, 맹견입마개, 실내허용
     */
    @ParameterizedTest(name = "contentId={0}")
    @CsvSource(nullValues = "", value = {
            // "전 견종 동반 가능" / 목줄 착용 / 맹견 입마개
            "1019041, ALL_AREAS,     ,   SMALL|MEDIUM|LARGE, , true, , , true, ",
            "129457,  ALL_AREAS,     ,   SMALL|MEDIUM|LARGE, , true, , , true, ",
            "2606741, ALL_AREAS,     ,   SMALL|MEDIUM|LARGE, , true, , , true, ",
            "2744501, ALL_AREAS,     ,   SMALL|MEDIUM|LARGE, , true, , , true, ",
            "2663350, PARTIAL_AREAS, ,   SMALL|MEDIUM|LARGE, , true, , , , ",
            // "10kg 이하 동반 가능"
            "129601,  PARTIAL_AREAS, 10, SMALL,              , true, , , , ",
            // 동반 가능 동물 비어 있음, 기타 정보에 "개목줄 착용"
            "1889094, PARTIAL_AREAS, ,   ,                   , true, , , , ",
            // "1인 당 최대 2마리" + 예방 접종 완료 + 리드줄
            "2740976, PARTIAL_AREAS, ,   ,                  2, true, , true, , ",
            // 정책 문구가 "타인에게 불편을 주지 않도록..."뿐 - 해석 가능한 항목 없음
            "3464160, ALL_AREAS,     ,   ,                   , , , , , ",
            // "15kg 미만 중소형견만(맹견 및 대형견 불가)" + 유모차/이동장 + "내부 실내 시설 입장 불가"
            "662268,  PARTIAL_AREAS, 15, SMALL|MEDIUM,       , , true, , , false",
    })
    @DisplayName("실제 detailPetTour2 응답 10건을 원문 그대로 해석한 결과가 기대값과 일치한다")
    void parse_RealSamples(String contentId, String scope, String maxWeight, String sizes, Integer maxCount,
                           Boolean leash, Boolean carrier, Boolean vaccination, Boolean muzzle, Boolean indoor) {
        ParsedPetPolicy p = parser.parse(REAL_SAMPLES.get(contentId));

        assertThat(p.accompanyScope()).isEqualTo(scope == null ? null : AccompanyScope.valueOf(scope));
        if (maxWeight == null) {
            assertThat(p.maxWeightLimitKg()).isNull();
        } else {
            assertThat(p.maxWeightLimitKg()).isEqualByComparingTo(maxWeight);
        }
        assertThat(p.allowedSizes()).isEqualTo(sizes == null ? null : List.of(sizes.split("\\|")));
        assertThat(p.maxPetCount()).isEqualTo(maxCount);
        assertThat(p.leashRequired()).isEqualTo(leash);
        assertThat(p.carrierRequired()).isEqualTo(carrier);
        assertThat(p.vaccinationRequired()).isEqualTo(vaccination);
        assertThat(p.muzzleRequiredForDangerousDogs()).isEqualTo(muzzle);
        assertThat(p.indoorAllowed()).isEqualTo(indoor);
    }

    @Test
    @DisplayName("원문은 해석 결과와 함께 그대로 보존된다 (화면 안내/재해석용)")
    void parse_KeepsOriginalText() {
        ParsedPetPolicy p = parser.parse(REAL_SAMPLES.get("662268"));

        assertThat(p.allowedPetsText()).isEqualTo("15kg 미만 중소형견만 입장 가능(맹견 및 대형견 입장 불가)");
        assertThat(p.requirementsText()).isEqualTo("반려동물 유모차 탑승,이동장(켄넬)사용");
        assertThat(p.etcText()).contains("내부 실내 시설 입장 불가");
    }

    @Test
    @DisplayName("item이 없거나 모든 문구가 비어 있으면 전 항목 미확인(null)이다 - 언급 없음을 '필요 없음'으로 해석하지 않는다")
    void parse_NullOrBlank_ReturnsUnknown() {
        assertThat(parser.parse(null)).isEqualTo(ParsedPetPolicy.unknown());
        assertThat(parser.parse(TourApiPetTourItemDto.builder().contentId("1").allowedPets(" ").etcInfo("").build()))
                .isEqualTo(ParsedPetPolicy.unknown());
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(delimiter = ';', nullValues = "", value = {
            "소형견만 동반 가능;              SMALL",
            "중소형견 동반 가능;              SMALL|MEDIUM",
            "대형견 입장 불가;                SMALL|MEDIUM",
            "모든 견종 가능;                  SMALL|MEDIUM|LARGE",
            "7kg 이하 소형견;                 SMALL",
            "25kg 이하;                       SMALL|MEDIUM|LARGE",
            "반려동물 동반 가능;              ",
    })
    @DisplayName("관찰 샘플 외의 크기/체중 표현도 PetSizeCategory 경계(10kg/20kg)에 맞게 해석한다")
    void parse_SizeExpressions(String allowedPets, String expectedSizes) {
        ParsedPetPolicy p = parser.parse(TourApiPetTourItemDto.builder().contentId("x").allowedPets(allowedPets).build());

        assertThat(p.allowedSizes()).isEqualTo(expectedSizes == null ? null : List.of(expectedSizes.trim().split("\\|")));
    }

    @Test
    @DisplayName("동반 가능 동물이 비어 있으면 같은 문구가 들어 있는 relaAcdntRiskMtr를 보조로 사용한다")
    void parse_FallsBackToAccidentRiskInfo() {
        ParsedPetPolicy p = parser.parse(TourApiPetTourItemDto.builder()
                .contentId("x").allowedPets("").accidentRiskInfo("10kg 이하 동반 가능").build());

        assertThat(p.maxWeightLimitKg()).isEqualByComparingTo("10");
        assertThat(p.allowedSizes()).containsExactly("SMALL");
    }
}
