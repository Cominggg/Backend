package com.Coming.Backend.common.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class MdcLoggingFilterTest {

    @InjectMocks
    private MdcLoggingFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Test
    void should_useHeaderTraceId_when_xRequestIdHeaderPresent() throws Exception {
        // given
        given(request.getHeader("X-Request-Id")).willReturn("test-trace-id");

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        verify(response).setHeader("X-Request-Id", "test-trace-id");
    }

    @Test
    void should_generateTraceId_when_xRequestIdHeaderAbsent() throws Exception {
        // given
        given(request.getHeader("X-Request-Id")).willReturn(null);

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        verify(response).setHeader(eq("X-Request-Id"), anyString());
    }

    @Test
    void should_clearMdcTraceId_after_requestCompletes() throws Exception {
        // given
        given(request.getHeader("X-Request-Id")).willReturn("cleanup-trace-id");

        // when
        filter.doFilterInternal(request, response, chain);

        // then
        assertThat(MDC.get("traceId")).isNull();
    }
}
