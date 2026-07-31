package com.Coming.Backend.common.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.common.discord.DiscordNotifier;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProxyManager<String> proxyManager;

    @Mock
    private BucketProxy bucket;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DiscordNotifier discordNotifier;

    private RateLimitFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(proxyManager, objectMapper, discordNotifier,
                new RateLimitFilter.RateLimitPolicy(20, 10, 1));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        given(proxyManager.builder().build(anyString(), any(java.util.function.Supplier.class))).willReturn(bucket);
    }

    @Test
    void should_passRequest_when_tokenAvailable() throws Exception {
        // given
        request.setRemoteAddr("127.0.0.1");
        given(bucket.tryConsume(1)).willReturn(true);
        FilterChain mockChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, mockChain);

        // then
        verify(mockChain).doFilter(request, response);
    }

    @Test
    void should_return429_when_rateLimitExceeded() throws Exception {
        // given
        request.setRemoteAddr("127.0.0.1");
        given(bucket.tryConsume(1)).willReturn(false);
        FilterChain mockChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, mockChain);

        // then
        verify(mockChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void should_useXForwardedForFirstIp_when_headerPresent() throws Exception {
        // given
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");
        given(bucket.tryConsume(1)).willReturn(true);
        FilterChain mockChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, mockChain);

        // then — X-Forwarded-For 첫 번째 값이 버킷 키로 사용됨
        verify(proxyManager.builder()).build(eq("RL:1.2.3.4"), any(java.util.function.Supplier.class));
    }

    @Test
    void should_useRemoteAddr_when_xForwardedForHeaderAbsent() throws Exception {
        // given
        request.setRemoteAddr("192.168.1.1");
        given(bucket.tryConsume(1)).willReturn(true);
        FilterChain mockChain = mock(FilterChain.class);

        // when
        filter.doFilterInternal(request, response, mockChain);

        // then — RemoteAddr가 버킷 키로 사용됨
        verify(proxyManager.builder()).build(eq("RL:192.168.1.1"), any(java.util.function.Supplier.class));
    }
}
