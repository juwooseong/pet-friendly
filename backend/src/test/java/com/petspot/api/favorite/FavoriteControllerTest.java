package com.petspot.api.favorite;

import com.petspot.api.favorite.dto.FavoriteResponseDto;
import com.petspot.application.favorite.FavoriteService;
import com.petspot.domain.user.entity.User;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.DuplicateFavoriteException;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteService favoriteService;

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

        mockUser = User.register("fav@petspot.com", "hash", "즐겨찾기유저");
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{placeId} 즐겨찾기 추가 201 Created 반환")
    void addFavorite_Success() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        FavoriteResponseDto response = FavoriteResponseDto.builder()
                .favoriteId(UUID.randomUUID())
                .placeId(placeId)
                .placeName("즐거운 동반 장소")
                .build();

        given(favoriteService.addFavorite(mockUser.getId(), placeId)).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/favorites/" + placeId)
                        .with(authentication(mockAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.placeName", is("즐거운 동반 장소")));
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{placeId} 이미 등록된 장소 추가 시 409 Conflict 반환")
    void addFavorite_Duplicate_Conflict() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        given(favoriteService.addFavorite(mockUser.getId(), placeId))
                .willThrow(new DuplicateFavoriteException("이미 즐겨찾기에 등록된 장소입니다."));

        // when & then
        mockMvc.perform(post("/api/v1/favorites/" + placeId)
                        .with(authentication(mockAuth)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("이미 즐겨찾기에 등록된 장소입니다.")));
    }

    @Test
    @DisplayName("DELETE /api/v1/favorites/{placeId} 즐겨찾기 삭제 200 OK 반환")
    void removeFavorite_Success() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/v1/favorites/" + placeId)
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/favorites 내 즐겨찾기 목록 조회 200 OK 반환")
    void getMyFavorites_Success() throws Exception {
        // given
        FavoriteResponseDto fav1 = FavoriteResponseDto.builder().placeName("장소1").build();
        FavoriteResponseDto fav2 = FavoriteResponseDto.builder().placeName("장소2").build();

        given(favoriteService.getMyFavorites(mockUser.getId())).willReturn(List.of(fav1, fav2));

        // when & then
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
