package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.social.AppleAuthClient.AppleProperties;
import com.Coming.Backend.auth.social.AppleAuthClient.AppleTokens;
import com.Coming.Backend.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class AppleAuthClientTest {

    private static final String BUNDLE_ID = "com.coming.app";
    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY1234567";
    private static final String AUTHORIZATION_CODE = "apple-authorization-code";
    private static final String APPLE_REFRESH_TOKEN = "apple-refresh-token";
    private static final String APPLE_ID_TOKEN = "apple-code-id-token";

    private static KeyPair keyPair;
    private static String pem;

    private final List<ClientRequest> requests = new ArrayList<>();

    @BeforeAll
    static void generateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded());
        pem = "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    private AppleAuthClient client(String privateKey, Function<ClientRequest, ClientResponse> responder) {
        WebClient webClient = WebClient.builder()
                .baseUrl(AppleAuthClient.BASE_URL)
                .exchangeFunction(request -> {
                    requests.add(request);
                    return Mono.just(responder.apply(request));
                })
                .build();
        return new AppleAuthClient(webClient, new AppleProperties(BUNDLE_ID, TEAM_ID, KEY_ID, privateKey));
    }

    private AppleAuthClient client(Function<ClientRequest, ClientResponse> responder) {
        return client(pem, responder);
    }

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private static Jws<Claims> parse(String jwt) {
        return Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(jwt);
    }

    private static MultiValueMap<String, String> formOf(ClientRequest request) {
        MockClientHttpRequest out = new MockClientHttpRequest(request.method(), request.url());
        request.body().insert(out, new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> hints() {
                return Map.of();
            }
        }).block();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (String pair : out.getBodyAsString().block().split("&")) {
            String[] keyValue = pair.split("=", 2);
            form.add(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
        }
        return form;
    }

    // -------------------------------------------------------------------------
    // createClientSecret
    // -------------------------------------------------------------------------

    @Test
    void should_create_es256_signed_client_secret_with_apple_claims() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.OK, "{}"));

        // when
        Jws<Claims> jws = parse(client.createClientSecret());

        // then
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(jws.getHeader().getKeyId()).isEqualTo(KEY_ID);
        assertThat(jws.getPayload().getIssuer()).isEqualTo(TEAM_ID);
        assertThat(jws.getPayload().getSubject()).isEqualTo(BUNDLE_ID);
        assertThat(jws.getPayload().getAudience()).containsExactly("https://appleid.apple.com");
        assertThat(jws.getPayload().getExpiration()).isAfter(jws.getPayload().getIssuedAt());
    }

    @Test
    void should_parse_private_key_when_pem_newlines_are_escaped_as_backslash_n() {
        // given — 환경변수로 들어온 PEM은 줄바꿈이 "\n" 문자열로 들어올 수 있다
        String escapedPem = pem.replace("\n", "\\n");
        AppleAuthClient client = client(escapedPem, request -> json(HttpStatus.OK, "{}"));

        // when
        Jws<Claims> jws = parse(client.createClientSecret());

        // then
        assertThat(jws.getPayload().getSubject()).isEqualTo(BUNDLE_ID);
    }

    @Test
    void should_throw_IllegalStateException_when_creating_client_secret_without_private_key() {
        // given — 키 설정이 비어 있어도 빈 생성은 성공한다
        AppleAuthClient client = client("", request -> json(HttpStatus.OK, "{}"));

        // when & then
        assertThatThrownBy(client::createClientSecret).isInstanceOf(IllegalStateException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void should_throw_IllegalStateException_on_construction_when_private_key_is_malformed() {
        // given
        String malformedPem = "-----BEGIN PRIVATE KEY-----\nnot-a-valid-key\n-----END PRIVATE KEY-----";

        // when & then
        assertThatThrownBy(() -> client(malformedPem, request -> json(HttpStatus.OK, "{}")))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------------------------
    // exchangeAuthorizationCode
    // -------------------------------------------------------------------------

    @Test
    void should_post_authorization_code_form_and_return_apple_tokens_when_exchange_succeeds() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.OK,
                "{\"access_token\": \"apple-access-token\", \"refresh_token\": \"" + APPLE_REFRESH_TOKEN
                        + "\", \"id_token\": \"" + APPLE_ID_TOKEN + "\", \"token_type\": \"bearer\", \"expires_in\": 3600}"));

        // when
        AppleTokens tokens = client.exchangeAuthorizationCode(AUTHORIZATION_CODE);

        // then
        assertThat(tokens.refreshToken()).isEqualTo(APPLE_REFRESH_TOKEN);
        assertThat(tokens.idToken()).isEqualTo(APPLE_ID_TOKEN);
        assertThat(requests).hasSize(1);
        ClientRequest request = requests.get(0);
        assertThat(request.method().name()).isEqualTo("POST");
        assertThat(request.url().toString()).isEqualTo("https://appleid.apple.com/auth/token");
        MultiValueMap<String, String> form = formOf(request);
        assertThat(form.getFirst("client_id")).isEqualTo(BUNDLE_ID);
        assertThat(form.getFirst("code")).isEqualTo(AUTHORIZATION_CODE);
        assertThat(form.getFirst("grant_type")).isEqualTo("authorization_code");
        assertThat(parse(form.getFirst("client_secret")).getPayload().getSubject()).isEqualTo(BUNDLE_ID);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_token_endpoint_returns_invalid_grant() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.BAD_REQUEST, "{\"error\": \"invalid_grant\"}"));

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(SocialTokenInvalidException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_IllegalStateException_when_token_endpoint_returns_invalid_client() {
        // given — client_secret·키 설정 문제는 사용자 오류가 아니라 서버 오류다
        AppleAuthClient client = client(request -> json(HttpStatus.BAD_REQUEST, "{\"error\": \"invalid_client\"}"));

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_throw_IllegalStateException_when_token_endpoint_returns_4xx_without_body() {
        // given
        AppleAuthClient client = client(request -> ClientResponse.create(HttpStatus.UNAUTHORIZED).build());

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_response_has_no_refresh_token() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.OK,
                "{\"access_token\": \"apple-access-token\", \"id_token\": \"" + APPLE_ID_TOKEN + "\"}"));

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(SocialTokenInvalidException.class);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_response_has_no_id_token() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.OK,
                "{\"access_token\": \"apple-access-token\", \"refresh_token\": \"" + APPLE_REFRESH_TOKEN + "\"}"));

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(SocialTokenInvalidException.class);
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_token_endpoint_returns_5xx() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(SocialProviderUnavailableException.class)
                .hasMessage(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE.getMessage());
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_token_response_body_is_empty() {
        // given
        AppleAuthClient client = client(request -> ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build());

        // when & then
        assertThatThrownBy(() -> client.exchangeAuthorizationCode(AUTHORIZATION_CODE))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }

    // -------------------------------------------------------------------------
    // revoke
    // -------------------------------------------------------------------------

    @Test
    void should_post_refresh_token_with_type_hint_to_revoke_endpoint_when_revoke() {
        // given
        AppleAuthClient client = client(request -> ClientResponse.create(HttpStatus.OK).build());

        // when
        client.revoke(APPLE_REFRESH_TOKEN);

        // then
        assertThat(requests).hasSize(1);
        ClientRequest request = requests.get(0);
        assertThat(request.method().name()).isEqualTo("POST");
        assertThat(request.url().toString()).isEqualTo("https://appleid.apple.com/auth/revoke");
        MultiValueMap<String, String> form = formOf(request);
        assertThat(form.getFirst("client_id")).isEqualTo(BUNDLE_ID);
        assertThat(form.getFirst("token")).isEqualTo(APPLE_REFRESH_TOKEN);
        assertThat(form.getFirst("token_type_hint")).isEqualTo("refresh_token");
        assertThat(form.getFirst("client_secret")).isNotBlank();
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_revoke_endpoint_returns_5xx() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.SERVICE_UNAVAILABLE, "{}"));

        // when & then
        assertThatThrownBy(() -> client.revoke(APPLE_REFRESH_TOKEN))
                .isInstanceOf(SocialProviderUnavailableException.class);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_revoke_endpoint_returns_invalid_grant() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.BAD_REQUEST, "{\"error\": \"invalid_grant\"}"));

        // when & then
        assertThatThrownBy(() -> client.revoke(APPLE_REFRESH_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class);
    }

    @Test
    void should_throw_IllegalStateException_when_revoke_endpoint_returns_invalid_client() {
        // given
        AppleAuthClient client = client(request -> json(HttpStatus.BAD_REQUEST, "{\"error\": \"invalid_client\"}"));

        // when & then
        assertThatThrownBy(() -> client.revoke(APPLE_REFRESH_TOKEN))
                .isInstanceOf(IllegalStateException.class);
    }
}
