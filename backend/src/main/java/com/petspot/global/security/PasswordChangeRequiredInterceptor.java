package com.petspot.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 임시 비밀번호로 로그인하여 강제 비밀번호 변경(passwordChangeRequired)이 필요한 사용자가
 * 비밀번호 변경 API 외의 인증 대상 API에 접근하는 것을 차단한다.
 * 실제 강제 변경 여부 판단(플래그 확인)만 담당하며, 비밀번호 변경 로직 자체는 AuthService가 담당한다.
 * /api/v1/auth/** (find-id, find-password, password 변경, 로그인/회원가입) 및 공개 API는
 * WebMvcConfig에서 excludePathPatterns로 제외되어 항상 접근 가능하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails
                && userDetails.isPasswordChangeRequired()) {

            log.warn("[PASSWORD CHANGE REQUIRED] Blocked access to {} {} for userId: {}",
                    request.getMethod(), request.getRequestURI(), userDetails.getId());

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("비밀번호 변경이 필요합니다. 먼저 비밀번호를 변경해주세요.")));
            return false;
        }

        return true;
    }
}
