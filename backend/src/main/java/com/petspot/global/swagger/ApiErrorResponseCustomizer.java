package com.petspot.global.swagger;

import com.petspot.global.config.WebMvcConfig;
import com.petspot.global.error.ErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Swagger(OpenAPI) 문서에 에러 응답 명세를 자동으로 채워 넣는다 (TSK-BE-023).
 *
 * <p>모든 에러 응답은 실제 런타임 응답과 동일한 공통 형식({@code ApiResponse.error(...)})을 따른다:
 * <pre>{"success": false, "data": null, "error": "에러 메시지", "timestamp": "..."}</pre>
 * 응답 본문에는 에러 코드 필드가 없으므로, 에러 코드(예: {@code PLACE_001})는 Swagger UI의 예시 이름으로만 노출된다.
 *
 * <p>동작 순서:
 * <ol>
 *   <li>{@link OperationCustomizer} 단계: 컨트롤러 메서드의 {@link ApiErrorCodes}를 읽어 operation 확장 필드
 *       ({@value #ERROR_CODES_EXTENSION})에 임시 저장한다. 이 단계에서는 응답(responses)을 직접 건드리지 않는다 -
 *       springdoc의 성공 응답(200/201) 자동 생성과 충돌하지 않게 하기 위함이다.</li>
 *   <li>{@link OpenApiCustomizer} 단계(문서 완성 직후): 경로/보안 정보를 기준으로 공통 에러를 더하고,
 *       위에서 저장한 비즈니스 에러와 합쳐 HTTP 상태 코드별 응답 + 에러 코드별 예시를 생성한다.</li>
 * </ol>
 *
 * <p>공통 에러 자동 적용 규칙:
 * <ul>
 *   <li>400 - 요청 본문이나 파라미터(경로/쿼리)가 있는 API (검증 실패, 잘못된 UUID/JSON 형식)</li>
 *   <li>401 - operation에 보안 요구사항이 선언된(인증 필요) API</li>
 *   <li>403 비밀번호 변경 필요 - 인증 필요 API 중 {@link WebMvcConfig#PASSWORD_CHANGE_CHECK_EXCLUDED_PATHS}에
 *       해당하지 않는 경로 (실제 인터셉터 설정과 같은 상수를 사용)</li>
 *   <li>500 - 전체 API</li>
 * </ul>
 */
@Component
public class ApiErrorResponseCustomizer implements OperationCustomizer, OpenApiCustomizer {

    /** OperationCustomizer -> OpenApiCustomizer 사이에서 비즈니스 에러 코드를 전달하는 임시 확장 필드명 */
    static final String ERROR_CODES_EXTENSION = "x-petspot-error-codes";

    /** components.schemas에 등록되는 공통 에러 응답 스키마 이름 */
    public static final String ERROR_RESPONSE_SCHEMA = "ErrorResponse";

    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
    private static final String EXAMPLE_TIMESTAMP = "2026-11-19T14:30:00.123456";
    private static final String VALIDATION_NOTE =
            "실제 error 메시지는 검증에 실패한 항목의 메시지로 내려온다(예: \"이메일은 필수 입력값입니다.\").";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // ---------------------------------------------------------------------
    // 1단계: 컨트롤러 메서드의 @ApiErrorCodes 수집
    // ---------------------------------------------------------------------
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiErrorCodes annotation = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
        if (annotation != null) {
            List<String> names = Arrays.stream(annotation.value()).map(Enum::name).toList();
            operation.addExtension(ERROR_CODES_EXTENSION, names);
        }
        return operation;
    }

    // ---------------------------------------------------------------------
    // 2단계: 문서 전체를 순회하며 에러 응답 생성
    // ---------------------------------------------------------------------
    @Override
    public void customise(OpenAPI openApi) {
        registerErrorResponseSchema(openApi);
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, pathItem) -> {
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
                applyErrorResponses(path, entry.getValue());
            }
        });
    }

    private void applyErrorResponses(String path, Operation operation) {
        Set<ErrorCode> codes = new LinkedHashSet<>();

        if (hasInput(operation)) {
            codes.add(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean secured = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
        if (secured) {
            codes.add(ErrorCode.UNAUTHENTICATED);
            if (isPasswordChangeChecked(path)) {
                codes.add(ErrorCode.PASSWORD_CHANGE_REQUIRED);
            }
        }
        codes.addAll(readDeclaredCodes(operation));
        codes.add(ErrorCode.INTERNAL_SERVER_ERROR);

        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        for (ErrorCode code : codes) {
            addErrorExample(operation.getResponses(), code);
        }
        sortResponsesByStatus(operation);
    }

    /** 요청 본문 또는 경로/쿼리 파라미터가 하나라도 있으면 입력값 검증 오류(400)가 발생할 수 있다. */
    private boolean hasInput(Operation operation) {
        if (operation.getRequestBody() != null) {
            return true;
        }
        List<Parameter> params = operation.getParameters();
        return params != null && params.stream()
                .anyMatch(p -> "path".equals(p.getIn()) || "query".equals(p.getIn()));
    }

    private boolean isPasswordChangeChecked(String path) {
        if (!pathMatcher.match(WebMvcConfig.PASSWORD_CHANGE_CHECK_PATH, path)) {
            return false;
        }
        return Arrays.stream(WebMvcConfig.PASSWORD_CHANGE_CHECK_EXCLUDED_PATHS)
                .noneMatch(excluded -> pathMatcher.match(excluded, path));
    }

    private List<ErrorCode> readDeclaredCodes(Operation operation) {
        if (operation.getExtensions() == null) {
            return List.of();
        }
        Object raw = operation.getExtensions().remove(ERROR_CODES_EXTENSION);
        if (operation.getExtensions().isEmpty()) {
            operation.setExtensions(null);
        }
        if (!(raw instanceof List<?> names)) {
            return List.of();
        }
        List<ErrorCode> result = new ArrayList<>();
        for (Object name : names) {
            result.add(ErrorCode.valueOf(String.valueOf(name)));
        }
        return result;
    }

    /**
     * 같은 HTTP 상태 코드의 에러는 하나의 응답으로 묶고, 에러 코드별로 예시를 하나씩 추가한다.
     * (예: 404 응답 안에 USER_001, PLACE_001 예시가 함께 표시됨)
     */
    private void addErrorExample(ApiResponses responses, ErrorCode code) {
        String status = String.valueOf(code.getStatus().value());
        ApiResponse response = responses.get(status);
        if (response == null) {
            response = new ApiResponse()
                    .description(code.getStatus().getReasonPhrase())
                    .content(new Content().addMediaType(JSON, new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_RESPONSE_SCHEMA))));
            responses.addApiResponse(status, response);
        }

        MediaType mediaType = response.getContent() != null ? response.getContent().get(JSON) : null;
        if (mediaType == null) {
            return; // 컨트롤러에서 직접 정의한 응답이면 덮어쓰지 않는다.
        }
        if (mediaType.getExamples() != null && mediaType.getExamples().containsKey(code.getCode())) {
            return; // 문서가 재생성될 때 같은 예시를 중복 추가하지 않는다.
        }

        String summary = code.getCode() + " (" + code.name() + ")";
        String detail = code == ErrorCode.INVALID_INPUT_VALUE ? VALIDATION_NOTE : null;
        mediaType.addExamples(code.getCode(), new Example()
                .summary(summary)
                .description(detail)
                .value(errorBody(code.getMessage())));

        String line = "- `" + code.getCode() + "` " + code.getMessage();
        response.setDescription(response.getDescription() + "\n\n" + line);
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("data", null);
        body.put("error", message);
        body.put("timestamp", EXAMPLE_TIMESTAMP);
        return body;
    }

    private void sortResponsesByStatus(Operation operation) {
        ApiResponses sorted = new ApiResponses();
        operation.getResponses().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> sorted.addApiResponse(e.getKey(), e.getValue()));
        operation.setResponses(sorted);
    }

    /** 공통 에러 응답 스키마 ({@code com.petspot.global.dto.ApiResponse}의 실패 형태)를 등록한다. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerErrorResponseSchema(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.components(new io.swagger.v3.oas.models.Components());
        }
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("success", new Schema<Boolean>().type("boolean").description("항상 false").example(false));
        properties.put("data", new Schema<>().type("object").nullable(true).description("항상 null"));
        properties.put("error", new Schema<String>().type("string").description("에러 메시지").example("장소를 찾을 수 없습니다."));
        properties.put("timestamp", new Schema<String>().type("string").format("date-time").description("응답 시각(서버 로컬 시간)"));

        Schema errorSchema = new Schema<>()
                .type("object")
                .description("공통 에러 응답 형식")
                .properties(properties);
        openApi.getComponents().addSchemas(ERROR_RESPONSE_SCHEMA, errorSchema);
    }
}
