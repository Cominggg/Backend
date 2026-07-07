package com.Coming.Backend.common.discord;

import com.Coming.Backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiscordNotificationServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private HttpServletRequest request;

    private DiscordNotificationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new DiscordNotificationService(webClientBuilder, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "fiveXxUrl", "https://discord.com/api/webhooks/5xx");
        ReflectionTestUtils.setField(service, "fourXxUrl", "https://discord.com/api/webhooks/4xx");

        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURI()).willReturn("/api/test");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    @Test
    void should_send_5xx_notification_when_cooldown_not_set() {
        // given
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);
        Exception exception = new RuntimeException("unexpected error");

        // when
        service.notifyFiveXx(request, exception);

        // then
        then(webClientBuilder).should().build();
    }

    @Test
    void should_skip_5xx_notification_when_cooldown_active() {
        // given
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(false);

        // when
        service.notifyFiveXx(request, new RuntimeException("error"));

        // then
        then(webClientBuilder).should(never()).build();
    }

    @Test
    void should_use_5min_cooldown_for_5xx() {
        // given
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);

        // when
        service.notifyFiveXx(request, new RuntimeException("error"));

        // then
        then(valueOperations).should().setIfAbsent(
                eq("discord:cooldown:5xx:RuntimeException"),
                eq("1"),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void should_send_4xx_notification_when_cooldown_not_set() {
        // given
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);

        // when
        service.notifyFourXx(request, ErrorCode.FORBIDDEN);

        // then
        then(webClientBuilder).should().build();
    }

    @Test
    void should_skip_4xx_notification_when_cooldown_active() {
        // given
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(false);

        // when
        service.notifyFourXx(request, ErrorCode.FORBIDDEN);

        // then
        then(webClientBuilder).should(never()).build();
    }

    @Test
    void should_use_1min_cooldown_for_4xx() {
        // given
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);

        // when
        service.notifyFourXx(request, ErrorCode.FORBIDDEN);

        // then
        then(valueOperations).should().setIfAbsent(
                eq("discord:cooldown:4xx:FORBIDDEN"),
                eq("1"),
                eq(Duration.ofMinutes(1))
        );
    }

    @Test
    void should_skip_send_when_5xx_url_is_blank() {
        // given
        ReflectionTestUtils.setField(service, "fiveXxUrl", "");
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);

        // when
        service.notifyFiveXx(request, new RuntimeException("error"));

        // then
        then(webClientBuilder).should(never()).build();
    }

    @Test
    void should_skip_send_when_4xx_url_is_blank() {
        // given
        ReflectionTestUtils.setField(service, "fourXxUrl", "");
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);

        // when
        service.notifyFourXx(request, ErrorCode.FORBIDDEN);

        // then
        then(webClientBuilder).should(never()).build();
    }
}
