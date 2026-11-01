package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class KakaoUnlinkClientTest {

    private static final String ADMIN_KEY = "kakao-admin-key";
    private static final String KAKAO_USER_ID = "99999";

    private final List<ClientRequest> requests = new ArrayList<>();

    private KakaoUnlinkClient client(HttpStatus status, String body) {
        WebClient webClient = WebClient.builder()
                .baseUrl(KakaoUnlinkClient.BASE_URL)
                .exchangeFunction(request -> {
                    requests.add(request);
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                })
                .build();
        return new KakaoUnlinkClient(webClient, ADMIN_KEY);
    }

    @Test
    void should_post_unlink_with_admin_key_when_unlink() {
        // given
        KakaoUnlinkClient client = client(HttpStatus.OK, "{\"id\": 99999}");

        // when
        client.unlink(KAKAO_USER_ID);

        // then
        assertThat(requests).hasSize(1);
        ClientRequest request = requests.get(0);
        assertThat(request.method()).isEqualTo(HttpMethod.POST);
        assertThat(request.url().getPath()).isEqualTo("/v1/user/unlink");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("KakaoAK " + ADMIN_KEY);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_kakao_rejects_request() {
        // given
        KakaoUnlinkClient client = client(HttpStatus.UNAUTHORIZED, "{\"code\": -401}");

        // when & then
        assertThatThrownBy(() -> client.unlink(KAKAO_USER_ID))
                .isInstanceOf(SocialTokenInvalidException.class);
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_kakao_server_fails() {
        // given
        KakaoUnlinkClient client = client(HttpStatus.INTERNAL_SERVER_ERROR, "{}");

        // when & then
        assertThatThrownBy(() -> client.unlink(KAKAO_USER_ID))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }
}
