package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * 소셜 제공자 HTTP 호출의 공통 오류 변환. 4xx는 토큰·코드가 잘못된 것으로(SOCIAL_TOKEN_INVALID),
 * 5xx·연결 실패·타임아웃·빈 응답은 제공자 장애로(SOCIAL_PROVIDER_UNAVAILABLE) 본다.
 */
@Slf4j
final class SocialProviderCalls {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private SocialProviderCalls() {
    }

    static WebClient.ResponseSpec mapStatus(WebClient.ResponseSpec spec, String path) {
        return spec
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.warn("소셜 제공자 요청 거절 — path: {}, status: {}", path, response.statusCode());
                    return Mono.error(new SocialTokenInvalidException());
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.warn("소셜 제공자 서버 오류 — path: {}, status: {}", path, response.statusCode());
                    return Mono.error(new SocialProviderUnavailableException());
                });
    }

    static <T> T block(Mono<T> call, String path) {
        T result = call.timeout(TIMEOUT)
                .onErrorMap(e -> e instanceof TimeoutException || e instanceof WebClientRequestException, e -> {
                    log.warn("소셜 제공자 연결 실패 — path: {}, error: {}", path, e.getMessage());
                    return new SocialProviderUnavailableException();
                })
                .block();
        if (result == null) {
            log.warn("소셜 제공자 빈 응답 — path: {}", path);
            throw new SocialProviderUnavailableException();
        }
        return result;
    }
}
