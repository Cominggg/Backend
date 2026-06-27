package com.Coming.Backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.auth.dto.NicknameCheckResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
import com.Coming.Backend.auth.dto.TokenResponse;
import com.Coming.Backend.auth.exception.NicknameDuplicateException;
import com.Coming.Backend.auth.exception.TermsNotAgreedException;
import com.Coming.Backend.auth.service.AuthService;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "new-access-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_PENDING"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/register
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_access_token_when_registration_is_valid() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(authService.register(eq(USER_ID), any(RegisterRequest.class)))
                .willReturn(new TokenResponse(ACCESS_TOKEN));

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN));
    }

    @Test
    void should_return_400_when_terms_not_agreed() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, false, true, false);
        given(authService.register(eq(USER_ID), any(RegisterRequest.class)))
                .willThrow(new TermsNotAgreedException());

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.TERMS_NOT_AGREED.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_409_when_nickname_is_duplicate() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(authService.register(eq(USER_ID), any(RegisterRequest.class)))
                .willThrow(new NicknameDuplicateException());

        // when & then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.NICKNAME_DUPLICATE.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/check-nickname
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_available_true_when_nickname_is_not_taken() throws Exception {
        // given
        given(authService.checkNickname("IU")).willReturn(new NicknameCheckResponse(true));

        // when & then
        mockMvc.perform(get("/api/auth/check-nickname")
                        .param("nickname", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void should_return_200_with_available_false_when_nickname_is_taken() throws Exception {
        // given
        given(authService.checkNickname("IU")).willReturn(new NicknameCheckResponse(false));

        // when & then
        mockMvc.perform(get("/api/auth/check-nickname")
                        .param("nickname", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
