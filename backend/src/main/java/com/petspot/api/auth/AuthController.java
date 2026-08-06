package com.petspot.api.auth;

import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.application.auth.AuthService;
import com.petspot.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 및 회원가입 REST Controller
 */
@Tag(name = "Auth API", description = "회원가입 및 JWT 인증 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "신규 회원가입",
            description = "이메일, 비밀번호, 닉네임을 입력받아 신규 회원을 등록합니다."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegisterResponseDto>> register(
            @Valid @RequestBody UserRegisterRequestDto request) {

        log.info("POST /api/v1/auth/register called with email: {}", request.getEmail());

        UserRegisterResponseDto result = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Operation(
            summary = "로그인 및 JWT 토큰 발급",
            description = "이메일과 비밀번호를 검증하여 JWT Access Token을 발급합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponseDto>> login(
            @Valid @RequestBody UserLoginRequestDto request) {

        log.info("POST /api/v1/auth/login called with email: {}", request.getEmail());

        UserLoginResponseDto result = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
