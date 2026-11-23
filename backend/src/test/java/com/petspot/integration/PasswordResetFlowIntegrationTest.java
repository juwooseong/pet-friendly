package com.petspot.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petspot.api.auth.dto.FindIdRequestDto;
import com.petspot.api.auth.dto.FindPasswordRequestDto;
import com.petspot.api.auth.dto.ResetPasswordRequestDto;
import com.petspot.api.auth.dto.UserLoginRequestDto;
import com.petspot.api.auth.dto.UserRegisterRequestDto;
import com.petspot.infrastructure.email.EmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 아이디 찾기 / 비밀번호 찾기(재설정 링크) 전체 흐름 통합 테스트 (TSK-SEC-003, 실DB).
 * <p>핵심: 비밀번호 찾기 요청만으로는 아무것도 바뀌지 않고, 메일 링크(1회용·30분)로 새 비밀번호를 정할 때만 바뀌며
 * 그때 기존 로그인 토큰이 모두 무효화된다(TSK-SEC-001).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetFlowIntegrationTest {

    private static final String ORIGINAL_PASSWORD = "OriginalPass1!";
    private static final String NEW_PASSWORD = "BrandNewPass2@";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private EmailSender emailSender;

    @Test
    @DisplayName("아이디 찾기 → 재설정 링크 요청(아무것도 안 바뀜) → 링크로 재설정 → 기존 토큰·비밀번호 무효 → 새 비밀번호 로그인 → 링크 재사용 불가")
    void findId_And_PasswordReset_FullFlow_Success() throws Exception {
        String email = "reset_flow@petspot.com";
        String nickname = "비번찾기유저";
        register(email, nickname);
        String oldToken = loginToken(email, ORIGINAL_PASSWORD);

        // 1. 아이디 찾기
        mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(FindIdRequestDto.builder().nickname(nickname).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedEmail", is("r*********@petspot.com")));

        // 2. 비밀번호 찾기 → 재설정 링크 메일 (응답에는 아무것도 없음)
        requestReset(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").doesNotExist());
        String link = sentLinks(email, 1).get(0);
        assertThat(link).contains("/reset-password?token=");

        // 3. 요청만으로는 비밀번호도, 로그인 상태도 바뀌지 않는다 (제3자 요청으로 계정이 잠기지 않음)
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());
        loginToken(email, ORIGINAL_PASSWORD);

        // 4. 링크의 토큰으로 새 비밀번호 설정
        resetPassword(tokenOf(link))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 5. 기존 로그인 토큰과 기존 비밀번호는 더 이상 쓸 수 없다
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        login(email, ORIGINAL_PASSWORD).andExpect(status().isUnauthorized());

        // 6. 새 비밀번호로 로그인하면 강제 변경 없이 바로 이용 가능
        MvcResult newLogin = login(email, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange", is(false)))
                .andReturn();
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(email)));

        // 7. 같은 링크는 다시 쓸 수 없다
        resetPassword(tokenOf(link))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("새 링크를 받으면 이전 링크는 폐기되고, 만료된 링크도 쓸 수 없다")
    void resetLink_ReplacedOrExpired_Rejected() throws Exception {
        String email = "reset_expire@petspot.com";
        register(email, "링크만료유저");

        requestReset(email).andExpect(status().isOk());
        requestReset(email).andExpect(status().isOk());
        List<String> links = sentLinks(email, 2);

        // 이전 링크는 새 링크 발급으로 폐기
        resetPassword(tokenOf(links.get(0))).andExpect(status().isBadRequest());

        // 최신 링크도 만료 시각이 지나면 거부
        entityManager.flush();
        jdbcTemplate.update("""
                UPDATE password_reset_tokens SET expires_at = now() - interval '1 minute'
                WHERE user_id = (SELECT id FROM users WHERE email = ?)""", email);
        entityManager.clear();
        resetPassword(tokenOf(links.get(1))).andExpect(status().isBadRequest());

        // 비밀번호는 그대로
        loginToken(email, ORIGINAL_PASSWORD);
    }

    @Test
    @DisplayName("같은 계정은 15분에 3회까지만 메일을 보내고, 초과해도 응답은 같다")
    void findPassword_RateLimited_SameResponse() throws Exception {
        String email = "reset_limit@petspot.com";
        register(email, "횟수제한유저");

        for (int i = 0; i < 4; i++) {
            requestReset(email)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }

        verify(emailSender, times(3)).sendPasswordResetLink(eq(email), anyString());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 찾기 요청 시에도 200 OK, 이메일 발송은 호출되지 않는다")
    void findPassword_NonExistentEmail_NoEmailSent() throws Exception {
        requestReset("no_such_user@petspot.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verifyNoInteractions(emailSender);
    }

    private void register(String email, String nickname) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UserRegisterRequestDto.builder()
                                .email(email).password(ORIGINAL_PASSWORD).nickname(nickname).build())))
                .andExpect(status().isCreated());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(UserLoginRequestDto.builder().email(email).password(password).build())));
    }

    private String loginToken(String email, String password) throws Exception {
        return accessToken(login(email, password).andExpect(status().isOk()).andReturn());
    }

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/find-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(FindPasswordRequestDto.builder().email(email).build())));
    }

    private ResultActions resetPassword(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(ResetPasswordRequestDto.builder()
                        .token(token).newPassword(NEW_PASSWORD).confirmPassword(NEW_PASSWORD).build())));
    }

    /** 이 이메일로 보낸 재설정 링크들 (EmailSender는 Mock이라 동기 호출) */
    private List<String> sentLinks(String email, int expectedCount) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, times(expectedCount)).sendPasswordResetLink(eq(email), captor.capture());
        return captor.getAllValues();
    }

    private static String tokenOf(String link) {
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    private String accessToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
