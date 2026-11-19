package com.petspot.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.global.config.SwaggerConfig;
import com.petspot.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * TSK-BE-023: Swagger(OpenAPI) 문서가 실제 API 동작과 일치하는지 검증한다.
 *
 * <p>문서는 사람이 손으로 관리하면 쉽게 실제 코드와 어긋나므로(예: 과거 보안 스킴 이름 대소문자 불일치로 Swagger UI에서
 * 토큰이 전송되지 않던 문제), 생성된 {@code /v3/api-docs}를 직접 읽어 다음을 고정한다:
 * <ul>
 *   <li>보안 표시(자물쇠)가 실제 Spring Security 설정과 일치하는지 - 토큰 없이 실제 호출해 401 여부 비교</li>
 *   <li>모든 API에 공통 에러(400/401/403/500)와 선언한 비즈니스 에러가 예시와 함께 붙는지</li>
 *   <li>생성 API가 실제 응답 코드(201)로 문서화되는지</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OpenApiDocumentationIntegrationTest {

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode apiDocs;

    @BeforeEach
    void loadApiDocs() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        apiDocs = objectMapper.readTree(json);
    }

    @Test
    @DisplayName("모든 API의 보안 요구사항은 정의된 보안 스킴만 참조하고, 문서 전역 보안은 걸려 있지 않다")
    void securityRequirements_ReferenceDefinedSchemeOnly() {
        assertThat(apiDocs.has("security")).as("전역 security가 있으면 공개 API에도 자물쇠가 표시된다").isFalse();
        assertThat(apiDocs.at("/components/securitySchemes").has(SwaggerConfig.SECURITY_SCHEME_NAME)).isTrue();

        for (OperationRef op : operations()) {
            JsonNode security = op.node().get("security");
            if (security == null) {
                continue;
            }
            for (JsonNode requirement : security) {
                requirement.fieldNames().forEachRemaining(name ->
                        assertThat(name).as("%s 의 보안 스킴 이름", op).isEqualTo(SwaggerConfig.SECURITY_SCHEME_NAME));
            }
        }
    }

    @Test
    @DisplayName("문서의 인증 필요 여부(자물쇠)가 실제 동작과 일치한다: 토큰 없이 호출 시 인증 API는 401, 공개 API는 401이 아니다")
    void documentedSecurity_MatchesActualSecurityBehavior() throws Exception {
        List<String> mismatches = new ArrayList<>();

        for (OperationRef op : operations()) {
            String url = op.path().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
            MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(op.method().toUpperCase()), url);
            if (op.node().has("requestBody")) {
                // 빈 본문: 공개 API는 검증 단계에서 400으로 끝나 데이터가 생성되지 않는다(테스트 트랜잭션도 롤백).
                builder.contentType(MediaType.APPLICATION_JSON).content("{}");
            }
            int status = mockMvc.perform(builder).andReturn().getResponse().getStatus();

            boolean documentedSecured = op.isSecured();
            boolean actuallySecured = status == 401;
            if (documentedSecured != actuallySecured) {
                mismatches.add(op + " documentedSecured=" + documentedSecured + " actualStatus=" + status);
            }
        }

        assertThat(mismatches).as("문서와 실제 보안 설정 불일치").isEmpty();
    }

    @Test
    @DisplayName("모든 API에 공통 에러 응답(500, 입력이 있으면 400, 인증 API면 401)이 공통 에러 스키마와 예시로 문서화된다")
    void commonErrorResponses_AreDocumentedForEveryOperation() {
        assertThat(apiDocs.at("/components/schemas/ErrorResponse/properties").has("error")).isTrue();

        for (OperationRef op : operations()) {
            assertHasExample(op, "500", ErrorCode.INTERNAL_SERVER_ERROR);

            boolean hasInput = op.node().has("requestBody") || op.node().has("parameters");
            if (hasInput) {
                assertHasExample(op, "400", ErrorCode.INVALID_INPUT_VALUE);
            }
            if (op.isSecured()) {
                assertHasExample(op, "401", ErrorCode.UNAUTHENTICATED);
            } else {
                // 공개 API에도 비즈니스 401(예: 로그인 실패 AUTH_002)은 있을 수 있지만, "인증 필요(AUTH_003)"는 없어야 한다
                assertThat(hasExample(op, "401", ErrorCode.UNAUTHENTICATED))
                        .as("%s 공개 API에는 인증 필요(401) 예시가 없어야 한다", op).isFalse();
            }
            assertThat(op.node().has("x-petspot-error-codes"))
                    .as("%s 내부 전달용 확장 필드가 문서에 남으면 안 된다", op).isFalse();
        }
    }

    @Test
    @DisplayName("비밀번호 강제 변경 403은 인터셉터 대상 API에만 문서화된다 (auth/places 경로 제외)")
    void passwordChangeRequired_DocumentedOnlyForInterceptedPaths() {
        for (OperationRef op : operations()) {
            boolean expected = op.isSecured()
                    && !op.path().startsWith("/api/v1/auth/")
                    && !op.path().startsWith("/api/v1/places/");
            boolean documented = hasExample(op, "403", ErrorCode.PASSWORD_CHANGE_REQUIRED);
            assertThat(documented).as("%s PASSWORD_CHANGE_REQUIRED 문서화 여부", op).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("@ApiErrorCodes로 선언한 비즈니스 에러가 상태 코드별로 묶여 문서화된다")
    void declaredBusinessErrors_AreGroupedByStatus() {
        OperationRef createReview = find("post", "/api/v1/reviews");
        assertHasExample(createReview, "404", ErrorCode.USER_NOT_FOUND);
        assertHasExample(createReview, "404", ErrorCode.PLACE_NOT_FOUND);
        assertHasExample(createReview, "409", ErrorCode.DUPLICATE_REVIEW);

        OperationRef updatePet = find("put", "/api/v1/pets/{petId}");
        assertHasExample(updatePet, "404", ErrorCode.PET_NOT_FOUND);
        // 같은 403 응답 안에 소유자 권한 오류와 비밀번호 변경 필요가 함께 예시로 표시된다
        assertHasExample(updatePet, "403", ErrorCode.PET_ACCESS_DENIED);
        assertHasExample(updatePet, "403", ErrorCode.PASSWORD_CHANGE_REQUIRED);

        OperationRef register = find("post", "/api/v1/auth/register");
        assertHasExample(register, "409", ErrorCode.DUPLICATE_EMAIL);
        assertHasExample(register, "409", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("생성 API는 실제 응답 코드인 201로 문서화된다")
    void createEndpoints_AreDocumentedAs201() {
        for (String[] target : new String[][]{
                {"post", "/api/v1/auth/register"},
                {"post", "/api/v1/pets"},
                {"post", "/api/v1/favorites/{placeId}"},
                {"post", "/api/v1/reviews"}}) {
            OperationRef op = find(target[0], target[1]);
            assertThat(op.responses().has("201")).as("%s 201", op).isTrue();
            assertThat(op.responses().has("200")).as("%s 200이 남으면 안 된다", op).isFalse();
        }
    }

    @Test
    @DisplayName("요청/응답 DTO 스키마에 설명과 예시 값이 포함된다")
    void dtoSchemas_HaveDescriptionsAndExamples() {
        JsonNode email = apiDocs.at("/components/schemas/UserRegisterRequestDto/properties/email");
        assertThat(email.path("example").asText()).isEqualTo("user@petspot.com");
        assertThat(email.path("description").asText()).isNotBlank();

        JsonNode allowedSizes = apiDocs.at("/components/schemas/PlaceSearchResponseDto/properties/allowedSizes");
        assertThat(allowedSizes.path("description").asText()).contains("null");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void assertHasExample(OperationRef op, String status, ErrorCode code) {
        assertThat(hasExample(op, status, code))
                .as("%s 의 %s 응답에 %s 예시가 있어야 한다", op, status, code.getCode())
                .isTrue();
        JsonNode example = op.responses().at("/" + status + "/content/application~1json/examples/" + code.getCode() + "/value");
        assertThat(example.path("success").asBoolean(true)).isFalse();
        assertThat(example.path("error").asText()).isEqualTo(code.getMessage());
    }

    private boolean hasExample(OperationRef op, String status, ErrorCode code) {
        return op.responses().at("/" + status + "/content/application~1json/examples").has(code.getCode());
    }

    private OperationRef find(String method, String path) {
        return operations().stream()
                .filter(op -> op.method().equals(method) && op.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("문서에 없는 API: " + method + " " + path));
    }

    private List<OperationRef> operations() {
        List<OperationRef> result = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> paths = apiDocs.get("paths").fields();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> path = paths.next();
            for (String method : HTTP_METHODS) {
                if (path.getValue().has(method)) {
                    result.add(new OperationRef(method, path.getKey(), path.getValue().get(method)));
                }
            }
        }
        assertThat(result).isNotEmpty();
        return result;
    }

    private record OperationRef(String method, String path, JsonNode node) {
        boolean isSecured() {
            return node.has("security") && !node.get("security").isEmpty();
        }

        JsonNode responses() {
            return node.path("responses");
        }

        @Override
        public String toString() {
            return method.toUpperCase() + " " + path;
        }
    }
}
