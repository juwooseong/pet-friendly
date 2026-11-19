package com.petspot.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger OpenAPI 명세서 및 Security (JWT Bearer) 설정
 *
 * <p><b>보안 표시 규칙 (TSK-BE-023):</b> 문서 전체(global)에 보안 요구사항을 걸지 않는다. 인증이 필요한 API만
 * {@code @Operation(security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME))}로 개별 선언하고,
 * 공개 API(회원가입/로그인/장소 조회 등)에는 자물쇠가 표시되지 않게 한다. 과거에는 global 보안(이름 "BearerAuth")과
 * 컨트롤러 선언(이름 "bearerAuth")의 대소문자가 달라 Swagger UI에서 Authorize 후에도 대부분의 API에 토큰이 전송되지
 * 않았다 - 이름을 이 상수 하나로 통일했다.
 *
 * <p>에러 응답 명세는 {@link com.petspot.global.swagger.ApiErrorResponseCustomizer}가 자동 생성한다.
 */
@Configuration
public class SwaggerConfig {

    /** 컨트롤러의 {@code @SecurityRequirement(name = ...)}에서 참조하는 JWT 보안 스킴 이름 */
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("로그인 API(POST /api/v1/auth/login) 응답의 accessToken 값을 입력 (\"Bearer \" 접두사 없이)"));

        return new OpenAPI()
                .info(new Info()
                        .title("PetSpot API Specification")
                        .description("""
                                반려동물 동반 장소 검색 및 관리 REST API 명세서

                                **공통 응답 형식**
                                - 성공: `{"success": true, "data": {...}, "error": null, "timestamp": "..."}`
                                - 실패: `{"success": false, "data": null, "error": "에러 메시지", "timestamp": "..."}`

                                **인증**: 자물쇠 표시가 있는 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.
                                임시 비밀번호로 로그인한 경우(`requiresPasswordChange=true`), 비밀번호 변경 전까지 대부분의 인증 API가 403을 반환합니다.

                                **에러 코드**: 각 에러 응답의 예시 이름(예: `PLACE_001`)은 서버 내부 에러 코드이며, 응답 본문에는 포함되지 않습니다.
                                """)
                        .version("v1.0.0"))
                .components(components);
    }
}
