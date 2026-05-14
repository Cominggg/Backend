package com.Coming.Backend.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.exception.InvalidTokenException;
import com.Coming.Backend.auth.repository.BlacklistRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final String ROLE = "USER";

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private Claims claims;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_setAuthentication_when_validTokenGiven() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        given(jwtProvider.parseClaims(VALID_TOKEN)).willReturn(claims);
        given(claims.getSubject()).willReturn(USER_ID.toString());
        given(claims.get("role", String.class)).willReturn(ROLE);
        given(blacklistRepository.isBlacklisted(VALID_TOKEN)).willReturn(false);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo(USER_ID);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_" + ROLE);
    }

    @Test
    void should_notSetAuthentication_when_tokenIsBlacklisted() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        given(jwtProvider.parseClaims(VALID_TOKEN)).willReturn(claims);
        given(blacklistRepository.isBlacklisted(VALID_TOKEN)).willReturn(true);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void should_passFilterWithoutAuthentication_when_tokenIsTampered() throws Exception {
        // given
        String tamperedToken = "tampered.jwt.token";
        request.addHeader("Authorization", "Bearer " + tamperedToken);
        given(jwtProvider.parseClaims(tamperedToken)).willThrow(new InvalidTokenException());

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void should_passFilterWithoutAuthentication_when_authorizationHeaderAbsent() throws Exception {
        // given — Authorization 헤더 없음

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void should_passFilterWithoutAuthentication_when_bearerPrefixMissing() throws Exception {
        // given
        request.addHeader("Authorization", VALID_TOKEN);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void should_invokeNextFilter_when_validTokenGiven() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        given(jwtProvider.parseClaims(VALID_TOKEN)).willReturn(claims);
        given(claims.getSubject()).willReturn(USER_ID.toString());
        given(claims.get("role", String.class)).willReturn(ROLE);
        given(blacklistRepository.isBlacklisted(VALID_TOKEN)).willReturn(false);

        FilterChain mockChain = org.mockito.Mockito.mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, mockChain);

        // then
        verify(mockChain).doFilter(request, response);
    }

    @Test
    void should_invokeNextFilter_when_authorizationHeaderAbsent() throws Exception {
        // given — Authorization 헤더 없음
        FilterChain mockChain = org.mockito.Mockito.mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, mockChain);

        // then
        verify(mockChain).doFilter(request, response);
    }
}
