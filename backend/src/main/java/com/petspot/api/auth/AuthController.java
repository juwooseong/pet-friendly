package com.petspot.api.auth;

import com.petspot.api.auth.dto.AvailabilityResponseDto;
import com.petspot.api.auth.dto.ChangePasswordRequestDto;
import com.petspot.api.auth.dto.FindIdRequestDto;
import com.petspot.api.auth.dto.FindIdResponseDto;
import com.petspot.api.auth.dto.FindPasswordRequestDto;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserLoginResponseDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.api.auth.dto.UserRegisterResponseDto;
import com.petspot.application.auth.AuthService;
import com.petspot.global.dto.ApiResponse;
import com.petspot.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 및 회원가입 REST Controller
 */
@Tag(name = "Auth API", description = "회원가입 및 JWT 인증 API")
@Slf4j
@Validated
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
            summary = "이메일 사용 가능 여부 확인 (실시간 중복확인)",
            description = "회원가입 화면에서 이메일 중복 여부를 실시간으로 확인합니다. " +
                    "이 응답은 참고용이며, 최종 중복 검사는 실제 회원가입(POST /register) 시점에 다시 수행됩니다."
    )
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<AvailabilityResponseDto>> checkEmail(
            @RequestParam
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email) {

        log.info("GET /api/v1/auth/check-email called");

        AvailabilityResponseDto result = authService.checkEmailAvailability(email);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "닉네임 사용 가능 여부 확인 (실시간 중복확인)",
            description = "회원가입 화면에서 닉네임 중복 여부를 실시간으로 확인합니다. " +
                    "이 응답은 참고용이며, 최종 중복 검사는 실제 회원가입(POST /register) 시점에 다시 수행됩니다."
    )
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<AvailabilityResponseDto>> checkNickname(
            @RequestParam
            @NotBlank(message = "닉네임은 필수 입력값입니다.")
            @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하이어야 합니다.")
            String nickname) {

        log.info("GET /api/v1/auth/check-nickname called");

        AvailabilityResponseDto result = authService.checkNicknameAvailability(nickname);

        return ResponseEntity.ok(ApiResponse.success(result));
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

    @Operation(
            summary = "아이디(이메일) 찾기",
            description = "가입 시 등록한 닉네임으로 계정을 조회하여 마스킹된 이메일 주소를 반환합니다. 일치하는 계정이 없으면 404를 반환합니다."
    )
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponseDto>> findId(
            @Valid @RequestBody FindIdRequestDto request) {

        log.info("POST /api/v1/auth/find-id called");

        FindIdResponseDto result = authService.findId(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(
            summary = "비밀번호 찾기 (임시 비밀번호 발급)",
            description = "이메일로 임시 비밀번호를 생성하여 발송합니다. 계정 존재 여부와 무관하게 항상 동일한 성공 응답을 반환합니다."
    )
    @PostMapping("/find-password")
    public ResponseEntity<ApiResponse<Void>> findPassword(
            @Valid @RequestBody FindPasswordRequestDto request) {

        log.info("POST /api/v1/auth/find-password called");

        authService.findPassword(request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 로그인한 사용자의 비밀번호를 변경합니다. 임시 비밀번호로 로그인한 사용자는 다른 API 이용 전 반드시 호출해야 합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto request) {

        log.info("PATCH /api/v1/auth/password called by userId: {}", userDetails.getId());

        authService.changePassword(userDetails.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
