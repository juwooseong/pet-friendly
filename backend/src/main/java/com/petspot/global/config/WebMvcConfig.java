package com.petspot.global.config;

import com.petspot.global.security.PasswordChangeRequiredInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC Interceptor 등록 설정
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 비밀번호 강제 변경 인터셉터 적용 경로 */
    public static final String PASSWORD_CHANGE_CHECK_PATH = "/api/v1/**";

    /**
     * 비밀번호 강제 변경 인터셉터 제외 경로.
     * Swagger 문서의 403(PASSWORD_CHANGE_REQUIRED) 표시 여부도 이 값을 그대로 사용한다(ApiErrorResponseCustomizer).
     */
    public static final String[] PASSWORD_CHANGE_CHECK_EXCLUDED_PATHS = {
            "/api/v1/auth/**",
            "/api/v1/places/**"
    };

    private final PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordChangeRequiredInterceptor)
                .addPathPatterns(PASSWORD_CHANGE_CHECK_PATH)
                .excludePathPatterns(PASSWORD_CHANGE_CHECK_EXCLUDED_PATHS);
    }
}
