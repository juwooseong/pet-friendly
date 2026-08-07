package com.petspot.api.pet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.pet.dto.PetCreateRequestDto;
import com.petspot.api.pet.dto.PetResponseDto;
import com.petspot.api.pet.dto.PetUpdateRequestDto;
import com.petspot.application.pet.PetService;
import com.petspot.domain.pet.entity.PetGender;
import com.petspot.domain.pet.entity.PetSizeCategory;
import com.petspot.domain.user.entity.User;
import com.petspot.global.config.SecurityConfig;
import com.petspot.global.error.exception.PetAccessDeniedException;
import com.petspot.global.error.exception.PetNotFoundException;
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
import java.time.LocalDate;
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

@WebMvcTest(PetController.class)
@Import({SecurityConfig.class, com.petspot.global.security.JwtAuthenticationFilter.class})
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PetService petService;

    @MockBean
    private com.petspot.global.util.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.petspot.global.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.petspot.global.security.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private User mockOwner;
    private UsernamePasswordAuthenticationToken mockAuth;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(401);
            response.getWriter().write("{\"success\":false,\"error\":\"인증이 필요한 요청입니다.\"}");
            return null;
        }).when(customAuthenticationEntryPoint).commence(any(), any(), any());

        mockOwner = User.register("owner@petspot.com", "hash", "보호자");
        CustomUserDetails userDetails = new CustomUserDetails(mockOwner);
        mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/pets 반려동물 등록 성공 시 201 Created 반환")
    void registerPet_Success() throws Exception {
        // given
        PetCreateRequestDto request = PetCreateRequestDto.builder()
                .name("초코")
                .breed("골든리트리버")
                .gender(PetGender.MALE)
                .birthDate(LocalDate.of(2022, 5, 1))
                .weightKg(new BigDecimal("25.0"))
                .neutered(true)
                .build();

        PetResponseDto response = PetResponseDto.builder()
                .id(UUID.randomUUID())
                .name("초코")
                .breed("골든리트리버")
                .gender(PetGender.MALE)
                .weightKg(new BigDecimal("25.0"))
                .sizeCategory(PetSizeCategory.LARGE)
                .neutered(true)
                .representative(true)
                .build();

        given(petService.registerPet(eq(mockOwner.getId()), any(PetCreateRequestDto.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/pets")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("초코")))
                .andExpect(jsonPath("$.data.sizeCategory", is("LARGE")))
                .andExpect(jsonPath("$.data.representative", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/pets 내 반려동물 목록 조회 성공 시 200 OK 반환")
    void getMyPets_Success() throws Exception {
        // given
        PetResponseDto pet1 = PetResponseDto.builder().id(UUID.randomUUID()).name("초코").representative(true).build();
        PetResponseDto pet2 = PetResponseDto.builder().id(UUID.randomUUID()).name("쿠키").representative(false).build();

        given(petService.getMyPets(mockOwner.getId())).willReturn(List.of(pet1, pet2));

        // when & then
        mockMvc.perform(get("/api/v1/pets")
                        .with(authentication(mockAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", is("초코")));
    }

    @Test
    @DisplayName("POST /api/v1/pets 등록 시 이름 누락/잘못된 체중 입력 경우 400 Bad Request 반환")
    void registerPet_InvalidValidation_BadRequest() throws Exception {
        // given
        PetCreateRequestDto invalidRequest = PetCreateRequestDto.builder()
                .name("") // 빈 이름
                .breed("")
                .weightKg(new BigDecimal("-1.0")) // 음수 체중
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/pets")
                        .with(authentication(mockAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("타인의 반려동물 접근 시 403 Forbidden 반환 (PetAccessDeniedException)")
    void getPet_AccessDenied_Forbidden() throws Exception {
        // given
        UUID otherPetId = UUID.randomUUID();
        given(petService.getPet(mockOwner.getId(), otherPetId))
                .willThrow(new PetAccessDeniedException("해당 반려동물 정보에 접근할 권한이 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/pets/" + otherPetId)
                        .with(authentication(mockAuth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", is("해당 반려동물 정보에 접근할 권한이 없습니다.")));
    }

    @Test
    @DisplayName("인증 없이 GET /api/v1/pets 접근 시 401 Unauthorized 반환")
    void getMyPets_Unauthenticated_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/pets"))
                .andExpect(status().isUnauthorized());
    }
}
