package com.Coming.Backend.auth.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.auth.dto.LoginNonceResponse;
import com.Coming.Backend.auth.dto.SocialLoginResponse;
import com.Coming.Backend.auth.dto.SocialTokenLoginRequest;
import com.Coming.Backend.auth.service.SocialLoginService;
import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SocialLoginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SocialLoginService socialLoginService;

    @InjectMocks
    private SocialLoginController socialLoginController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ACCESS_TOKEN = "new-access-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(socialLoginController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login/nonce
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_nonce_when_issue_login_nonce() throws Exception {
        // given
        String nonce = "q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c";
        given(socialLoginService.issueNonce()).willReturn(new LoginNonceResponse(nonce));

        // when & then
        mockMvc.perform(post("/api/auth/login/nonce"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonce").value(nonce));
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login/{provider}/token
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_tokens_and_isNewUser_when_social_token_login_succeeds() throws Exception {
        // given
        SocialTokenLoginRequest request = new SocialTokenLoginRequest("google-id-token", null);
        given(socialLoginService.login("google", request))
                .willReturn(new SocialLoginResponse(ACCESS_TOKEN, "new-refresh-token", true));

        // when & then
        mockMvc.perform(post("/api/auth/login/google/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.isNewUser").value(true));
    }

    @Test
    void should_return_400_when_social_token_is_blank() throws Exception {
        // given
        SocialTokenLoginRequest request = new SocialTokenLoginRequest("", null);

        // when & then
        mockMvc.perform(post("/api/auth/login/google/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        verifyNoInteractions(socialLoginService);
    }
}
