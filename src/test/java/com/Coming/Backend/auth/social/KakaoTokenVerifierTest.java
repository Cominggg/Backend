package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.oauth2.KakaoOAuth2UserInfo;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.common.exception.ErrorCode;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

class KakaoTokenVerifierTest {

    private static final String APP_ID = "123456";
    private static final String ACCESS_TOKEN = "kakao-access-token";
    private static final String TOKEN_INFO_PATH = "/v1/user/access_token_info";
    private static final String USER_ME_PATH = "/v2/user/me";

    private final List<ClientRequest> requests = new ArrayList<>();

    private KakaoTokenVerifier verifier(String appId, Function<ClientRequest, ClientResponse> responder) {
        return exchangingVerifier(appId, request -> Mono.just(responder.apply(request)));
    }

    private KakaoTokenVerifier exchangingVerifier(String appId, Function<ClientRequest, Mono<ClientResponse>> exchange) {
        WebClient webClient = WebClient.builder()
                .baseUrl(KakaoTokenVerifier.BASE_URL)
                .exchangeFunction(request -> {
                    requests.add(request);
                    return exchange.apply(request);
                })
                .build();
        return new KakaoTokenVerifier(webClient, appId);
    }

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private static Function<ClientRequest, ClientResponse> kakaoApi(String tokenInfoAppId) {
        return request -> switch (request.url().getPath()) {
            case TOKEN_INFO_PATH -> json(HttpStatus.OK, "{\"id\": 99999, \"app_id\": " + tokenInfoAppId + "}");
            case USER_ME_PATH -> json(HttpStatus.OK,
                    "{\"id\": 99999, \"kakao_account\": {\"email\": \"iu@kakao.com\"}}");
            default -> json(HttpStatus.NOT_FOUND, "{}");
        };
    }

    @Test
    void should_return_kakao_as_provider() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID, kakaoApi(APP_ID));

        // when & then
        assertThat(verifier.provider()).isEqualTo("kakao");
    }

    @Test
    void should_return_kakao_user_info_from_user_me_when_app_id_matches() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID, kakaoApi(APP_ID));

        // when
        OAuth2UserInfo userInfo = verifier.verify(ACCESS_TOKEN);

        // then
        assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
        assertThat(userInfo.getProviderId()).isEqualTo("99999");
        assertThat(userInfo.getEmail()).isEqualTo("iu@kakao.com");
    }

    @Test
    void should_send_bearer_authorization_header_to_both_apis_when_verify() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID, kakaoApi(APP_ID));

        // when
        verifier.verify(ACCESS_TOKEN);

        // then
        assertThat(requests)
                .extracting(request -> request.url().getPath())
                .containsExactly(TOKEN_INFO_PATH, USER_ME_PATH);
        assertThat(requests)
                .extracting(request -> request.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .containsOnly("Bearer " + ACCESS_TOKEN);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_app_id_does_not_match() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID, kakaoApi("654321"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_INVALID.getMessage());
        assertThat(requests).extracting(request -> request.url().getPath()).containsExactly(TOKEN_INFO_PATH);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_app_id_setting_is_blank() {
        // given
        KakaoTokenVerifier verifier = verifier("", kakaoApi(APP_ID));

        // when & then
        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class);
        assertThat(requests).extracting(request -> request.url().getPath()).containsExactly(TOKEN_INFO_PATH);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_token_info_api_returns_401() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID,
                request -> json(HttpStatus.UNAUTHORIZED, "{\"msg\": \"this access token does not exist\", \"code\": -401}"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_INVALID.getMessage());
        assertThat(requests).extracting(request -> request.url().getPath()).containsExactly(TOKEN_INFO_PATH);
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_kakao_api_returns_5xx() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID, request -> json(HttpStatus.SERVICE_UNAVAILABLE, "{}"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(SocialProviderUnavailableException.class)
                .hasMessage(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE.getMessage());
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_connection_to_kakao_fails() {
        // given
        KakaoTokenVerifier verifier = exchangingVerifier(APP_ID, request -> Mono.error(
                new WebClientRequestException(new ConnectException("Connection refused"),
                        HttpMethod.GET, request.url(), new HttpHeaders())));

        // when & then
        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_kakao_response_body_is_empty() {
        // given
        KakaoTokenVerifier verifier = verifier(APP_ID, request -> ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build());

        // when & then
        assertThatThrownBy(() -> verifier.verify(ACCESS_TOKEN))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }
}
