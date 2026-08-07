package com.petspot.api.dashboard;

import com.petspot.api.dashboard.dto.DashboardResponseDto;
import com.petspot.api.user.dto.MyPageResponseDto.RepresentativePetDto;
import com.petspot.application.dashboard.DashboardQueryService;
import com.petspot.domain.user.entity.User;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardQueryService dashboardQueryService;

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

        mockUser = User.register("dash@petspot.com", "hash", "대시보드유저");
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("GET /api/v1/dashboard 인증 유저 요청 시 200 OK 및 DashboardResponseDto 반환")
    void getDashboardSummary_Success() throws Exception {
        // given
        RepresentativePetDto repPet = RepresentativePetDto.builder()
                .petId(UUID.randomUUID())
                .petName("코코")
                .breed("말티즈")
                .build();

        DashboardResponseDto mockResponse = DashboardResponseDto.builder()
                .nickname("대시보드유저")
                .representativePet(repPet)
                .favoriteCount(4)
                .popularPlaces(Collections.emptyList())
                .recentPlaces(Collections.emptyList())
                .recommendedPlaces(Collections.emptyList())
                .build();

        given(dashboardQueryService.getDashboardSummary(mockUser.getId())).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.nickname", is("대시보드유저")))
                .andExpect(jsonPath("$.data.representativePet.petName", is("코코")))
                .andExpect(jsonPath("$.data.favoriteCount", is(4)));
    }

    @Test
    @DisplayName("인증 헤더 없이 GET /api/v1/dashboard 접근 시 401 Unauthorized 반환")
    void getDashboardSummary_Unauthenticated_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
