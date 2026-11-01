package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.common.exception.ErrorCode;
import java.net.URI;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

class SocialProviderCallsTest {

    private static final String PATH = "/v2/user/me";

    @Test
    void should_return_value_when_call_emits_value() {
        // given
        Mono<String> call = Mono.just("response");

        // when
        String result = SocialProviderCalls.block(call, PATH);

        // then
        assertThat(result).isEqualTo("response");
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_call_times_out() {
        // given
        Mono<String> call = Mono.error(new TimeoutException("Did not observe any item within 5000ms"));

        // when & then
        assertThatThrownBy(() -> SocialProviderCalls.block(call, PATH))
                .isInstanceOf(SocialProviderUnavailableException.class)
                .hasMessage(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE.getMessage());
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_connection_fails() {
        // given
        Mono<String> call = Mono.error(new WebClientRequestException(new ConnectException("Connection refused"),
                HttpMethod.GET, URI.create("https://kapi.kakao.com" + PATH), new HttpHeaders()));

        // when & then
        assertThatThrownBy(() -> SocialProviderCalls.block(call, PATH))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_response_is_empty() {
        // given
        Mono<String> call = Mono.empty();

        // when & then
        assertThatThrownBy(() -> SocialProviderCalls.block(call, PATH))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }

    @Test
    void should_rethrow_SocialTokenInvalidException_as_is_when_status_mapping_rejected_request() {
        // given
        Mono<String> call = Mono.error(new SocialTokenInvalidException());

        // when & then
        assertThatThrownBy(() -> SocialProviderCalls.block(call, PATH))
                .isInstanceOf(SocialTokenInvalidException.class);
    }
}
