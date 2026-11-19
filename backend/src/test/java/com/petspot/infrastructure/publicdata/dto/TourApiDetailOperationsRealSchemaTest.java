package com.petspot.infrastructure.publicdata.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TSK-DATA-005: detailIntro2 / detailInfo2 / detailCommon2 실제 응답 fixture(2026-09-14, 실제 호출로 확보,
 * serviceKey 미포함)를 사용한 오프라인 스키마 검증. 네트워크/실제 키 없이 동작한다.
 *
 * <p>이 fixture들은 {@link TourApiResponseDto}/{@link TourApiPlaceItemDto}가 표현하는 areaBasedList2 전용
 * item 스키마와 필드 구성이 다르므로(오퍼레이션마다 필드셋이 완전히 다름), 여기서는 JsonNode로 실제 필드
 * 존재/부재만 검증한다 - 존재하지 않는 것으로 확인된 반려동물 추측 필드(acmpyTypeCd 등)를 기존 DTO로 억지로
 * 매핑하지 않는다.
 */
class TourApiDetailOperationsRealSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] GUESSED_PET_FIELDS = {
            "acmpyTypeCd", "claAcmpyInfo", "relaAcmpyInfo", "relaPosbleFclty",
            "relaRqustFclty", "etcAcmpyInfo", "acmpyPsblCpam"
    };

    @Test
    @DisplayName("detailIntro2(쇼핑, contentTypeId=38): chkpetshopping 필드만 존재하고, 기존 추측 필드는 전혀 없다")
    void detailIntro2_shopping_hasChkpetFieldOnly_noGuessedFields() throws IOException {
        JsonNode item = firstItem("success-detail-intro2-real-2026-09-14.json");

        assertThat(item.get("contentid").asText()).isEqualTo("2930927");
        assertThat(item.get("contenttypeid").asText()).isEqualTo("38");
        assertThat(item.has("chkpetshopping")).as("쇼핑 타입은 chkpetshopping 필드를 갖는다").isTrue();
        // 실제 값은 빈 문자열 - 이 항목은 반려동물 동반 가능 여부가 미기재 상태임을 의미한다.
        assertThat(item.get("chkpetshopping").asText()).isEmpty();

        assertNoGuessedPetFields(item);
    }

    @Test
    @DisplayName("detailIntro2(숙박, contentTypeId=32): chkpet 계열 필드 자체가 존재하지 않는다")
    void detailIntro2_lodging_hasNoChkpetField_noGuessedFields() throws IOException {
        JsonNode item = firstItem("success-detail-intro2-lodging-real-2026-09-14.json");

        assertThat(item.get("contenttypeid").asText()).isEqualTo("32");
        item.fieldNames().forEachRemaining(field ->
                assertThat(field.toLowerCase()).as("숙박 detailIntro2 응답에 chkpet류 필드가 없어야 한다")
                        .doesNotContain("pet"));

        assertNoGuessedPetFields(item);
    }

    @Test
    @DisplayName("detailInfo2(쇼핑, contentTypeId=38): 이 contentId는 반복정보 자체가 없다(items가 빈 문자열)")
    void detailInfo2_shopping_hasNoRepeatInfo() throws IOException {
        JsonNode root = readFixture("success-detail-info2-real-2026-09-14.json");
        JsonNode items = root.path("response").path("body").path("items");

        assertThat(items.isTextual()).as("데이터가 없으면 items가 객체가 아닌 빈 문자열로 내려온다").isTrue();
        assertThat(items.asText()).isEmpty();
        assertThat(root.path("response").path("body").path("totalCount").asInt()).isZero();
    }

    @Test
    @DisplayName("detailInfo2(숙박, contentTypeId=32): 반복정보는 객실별 상세이며 반려동물 필드가 없다")
    void detailInfo2_lodging_isRoomInfo_noGuessedFields() throws IOException {
        JsonNode item = firstItem("success-detail-info2-lodging-real-2026-09-14.json");

        assertThat(item.has("roomtitle")).isTrue();
        assertThat(item.has("roomcount")).isTrue();
        assertNoGuessedPetFields(item);
    }

    @Test
    @DisplayName("detailCommon2: mapx/mapy가 실제로 숫자 변환 가능한 경도/위도 값이다")
    void detailCommon2_mapxMapy_areParseableCoordinates() throws IOException {
        JsonNode item = firstItem("success-detail-common2-real-2026-09-14.json");

        assertThat(item.get("contentid").asText()).isEqualTo("2930927");
        String mapx = item.get("mapx").asText();
        String mapy = item.get("mapy").asText();

        assertThat(Double.parseDouble(mapx)).isEqualTo(129.056805686045);
        assertThat(Double.parseDouble(mapy)).isEqualTo(35.1579550616706);
        // areaBasedList2 fixture(success-area-based-list2-real-2026-09-09.json)의 mapx/mapy와 동일한 값 -
        // 두 오퍼레이션 사이에 좌표 데이터 중복이 있음을 확인.
        assertThat(mapx).isEqualTo("129.056805686045");
        assertThat(mapy).isEqualTo("35.1579550616706");

        assertNoGuessedPetFields(item);
    }

    private void assertNoGuessedPetFields(JsonNode item) {
        for (String field : GUESSED_PET_FIELDS) {
            assertThat(item.has(field)).as("추측 필드 '%s'는 실제 응답에 존재하지 않아야 한다", field).isFalse();
        }
    }

    private JsonNode firstItem(String fileName) throws IOException {
        JsonNode root = readFixture(fileName);
        JsonNode itemNode = root.path("response").path("body").path("items").path("item");
        assertThat(itemNode.isArray() && itemNode.size() > 0).as("fixture에 item 배열이 있어야 한다").isTrue();
        return itemNode.get(0);
    }

    private JsonNode readFixture(String fileName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("tourapi/" + fileName)) {
            assertThat(is).as("fixture file must exist: " + fileName).isNotNull();
            return objectMapper.readTree(is);
        }
    }
}
