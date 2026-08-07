package com.petspot.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.user.dto.MyPageResponseDto;
import com.petspot.api.user.dto.UserProfileResponseDto;
import com.petspot.api.user.dto.UserProfileUpdateRequestDto;
import com.petspot.application.user.MyPageQueryService;
import com.petspot.application.user.UserProfileService;
import com.petspot.domain.pet.entity.PetSizeCategory;
import com.petspot.domain.user.entity.User;
import com.petspot.domain.user.entity.UserRole;
import com.petspot.domain.user.entity.UserStatus;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.UserNotFoundException;
import com.petspot.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private MyPageQueryService myPageQueryService;

    @MockBean
    private com.petspot.global.util.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.petspot.global.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.petspot.global.security.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private User mockUser;
    private UsernamePasswordAuthenticationToken mockAuth;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(401);
            response.getWriter().write("{\"success\":false,\"error\":\"인증이 필요한 요청입니다.\"}");
            return null;
        }).when(customAuthenticationEntryPoint).commence(any(), any(), any());

        mockUser = User.register("me@petspot.com", "hash", "내닉네임", "https://example.com/avatar.jpg");
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("GET /api/v1/users/me 인증 유저 요청 시 200 OK 및 MyPageResponseDto 반환")
    void getMyPageSummary_Success() throws Exception {
        // given
        MyPageResponseDto.RepresentativePetDto repPet = MyPageResponseDto.RepresentativePetDto.builder()
                .petId(UUID.randomUUID())
                .petName("초코")
                .breed("푸들")
                .weightKg(new BigDecimal("5.5"))
                .sizeCategory(PetSizeCategory.SMALL)
                .build();

        MyPageResponseDto mockResponse = MyPageResponseDto.builder()
                .userId(mockUser.getId())
                .email("me@petspot.com")
                .nickname("내닉네임")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(UserRole.USER)
                .representativePet(repPet)
                .petCount(2)
                .favoriteCount(5)
                .reviewCount(3)
                .build();

        given(myPageQueryService.getMyPageSummary(mockUser.getId())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("me@petspot.com")))
                .andExpect(jsonPath("$.data.nickname", is("내닉네임")))
                .andExpect(jsonPath("$.data.representativePet.petName", is("초코")))
                .andExpect(jsonPath("$.data.petCount", is(2)))
                .andExpect(jsonPath("$.data.favoriteCount", is(5)))
                .andExpect(jsonPath("$.data.reviewCount", is(3)));
    }

    @Test
    @DisplayName("GET /api/v1/users/me 존재하지 않는 회원의 요청 시 404 Not Found 반환")
    void getMyPageSummary_UserNotFound_NotFound() throws Exception {
        // given
        given(myPageQueryService.getMyPageSummary(mockUser.getId()))
                .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(mockAuth)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("사용자를 찾을 수 없습니다.")));
    }

    @Test
    @DisplayName("인증 헤더 없이 GET /api/v1/users/me 접근 시 401 Unauthorized 반환")
    void getMyPageSummary_Unauthenticated_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me 인증 유저 프로필 수정 시 200 OK 반환")
    void updateMyProfile_Success() throws Exception {
        // given
        UserProfileUpdateRequestDto updateRequest = UserProfileUpdateRequestDto.builder()
                .nickname("새로운닉네임")
                .avatarUrl("https://example.com/new_avatar.jpg")
                .build();

        UserProfileResponseDto mockResponse = UserProfileResponseDto.builder()
                .id(mockUser.getId())
                .email("me@petspot.com")
                .nickname("새로운닉네임")
                .avatarUrl("https://example.com/new_avatar.jpg")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userProfileService.updateProfile(any(), any())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(put("/api/v1/users/me")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.nickname", is("새로운닉네임")))
                .andExpect(jsonPath("$.data.avatarUrl", is("https://example.com/new_avatar.jpg")));
    }
}
