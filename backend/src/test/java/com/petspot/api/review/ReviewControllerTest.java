package com.petspot.api.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.review.dto.ReviewCreateRequestDto;
import com.petspot.api.review.dto.ReviewResponseDto;
import com.petspot.api.review.dto.ReviewUpdateRequestDto;
import com.petspot.application.review.ReviewService;
import com.petspot.domain.user.entity.User;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.DuplicateReviewException;
import com.petspot.global.error.exception.ReviewAccessDeniedException;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

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

        mockUser = User.register("reviewer@petspot.com", "hash", "리뷰어");
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/reviews 리뷰 등록 성공 시 201 Created 반환")
    void createReview_Success() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        ReviewCreateRequestDto request = ReviewCreateRequestDto.builder()
                .placeId(placeId)
                .rating(5)
                .content("최고의 애견 동반 카페입니다!")
                .build();

        ReviewResponseDto response = ReviewResponseDto.builder()
                .id(UUID.randomUUID())
                .placeId(placeId)
                .rating(5)
                .content("최고의 애견 동반 카페입니다!")
                .userNickname("리뷰어")
                .build();

        given(reviewService.createReview(eq(mockUser.getId()), any(ReviewCreateRequestDto.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andExpect(jsonPath("$.data.userNickname", is("리뷰어")));
    }

    @Test
    @DisplayName("GET /api/v1/places/{placeId}/reviews 특정 장소 리뷰 목록 조회 200 OK 반환")
    void getPlaceReviews_Success() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        ReviewResponseDto review1 = ReviewResponseDto.builder().id(UUID.randomUUID()).rating(5).content("좋아요1").build();
        ReviewResponseDto review2 = ReviewResponseDto.builder().id(UUID.randomUUID()).rating(4).content("좋아요2").build();

        given(reviewService.getPlaceReviews(placeId)).willReturn(List.of(review1, review2));

        // when & then
        mockMvc.perform(get("/api/v1/places/" + placeId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/v1/reviews 이미 작성된 장소 중복 리뷰 등록 시 409 Conflict 반환")
    void createReview_Duplicate_Conflict() throws Exception {
        // given
        UUID placeId = UUID.randomUUID();
        ReviewCreateRequestDto request = ReviewCreateRequestDto.builder()
                .placeId(placeId)
                .rating(5)
                .content("중복 작성 시도")
                .build();

        given(reviewService.createReview(eq(mockUser.getId()), any(ReviewCreateRequestDto.class)))
                .willThrow(new DuplicateReviewException("이미 이 장소에 리뷰를 작성하셨습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/reviews")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("이미 이 장소에 리뷰를 작성하셨습니다.")));
    }

    @Test
    @DisplayName("타인의 리뷰 수정 시 403 Forbidden 반환 (ReviewAccessDeniedException)")
    void updateReview_AccessDenied_Forbidden() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();
        ReviewUpdateRequestDto request = ReviewUpdateRequestDto.builder()
                .rating(5)
                .content("타인 리뷰 수정시도")
                .build();

        given(reviewService.updateReview(eq(mockUser.getId()), eq(reviewId), any(ReviewUpdateRequestDto.class)))
                .willThrow(new ReviewAccessDeniedException("해당 리뷰를 수정 또는 삭제할 권한이 없습니다."));

        // when & then
        mockMvc.perform(put("/api/v1/reviews/" + reviewId)
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("해당 리뷰를 수정 또는 삭제할 권한이 없습니다.")));
    }

    @Test
    @DisplayName("인증 없이 POST /api/v1/reviews 접근 시 401 Unauthorized 반환")
    void createReview_Unauthenticated_Unauthorized() throws Exception {
        // given
        ReviewCreateRequestDto request = ReviewCreateRequestDto.builder()
                .placeId(UUID.randomUUID())
                .rating(5)
                .content("미인증 접근")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
