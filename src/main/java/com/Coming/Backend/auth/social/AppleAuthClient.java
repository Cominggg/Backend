package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import io.jsonwebtoken.Jwts;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Apple 인증 서버 호출(authorizationCode 교환, 토큰 revoke). 요청마다 .p8 키로 서명한 client_secret JWT를 쓴다.
 */
@Slf4j
@Component
public class AppleAuthClient {

    static final String BASE_URL = "https://appleid.apple.com";
    private static final String TOKEN_PATH = "/auth/token";
    private static final String REVOKE_PATH = "/auth/revoke";
    private static final String INVALID_GRANT = "invalid_grant";
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final AppleProperties properties;
    private final PrivateKey privateKey;

    @Autowired
    public AppleAuthClient(
            WebClient.Builder builder,
            @Value("${app.oauth2.apple.bundle-id}") String bundleId,
            @Value("${app.oauth2.apple.team-id}") String teamId,
            @Value("${app.oauth2.apple.key-id}") String keyId,
            @Value("${app.oauth2.apple.private-key}") String privateKey) {
        this(builder.baseUrl(BASE_URL).build(), new AppleProperties(bundleId, teamId, keyId, privateKey));
    }

    AppleAuthClient(WebClient webClient, AppleProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        this.privateKey = properties.privateKey().isBlank() ? null : parsePrivateKey(properties.privateKey());
    }

    record AppleProperties(String bundleId, String teamId, String keyId, String privateKey) {}

    /**
     * authorizationCode 교환 결과. idToken은 코드를 발급받은 Apple 사용자를 확인하는 데 쓴다.
     */
    public record AppleTokens(String refreshToken, String idToken) {}

    /**
     * 로그인 시 받은 1회용 authorizationCode를 Apple refresh_token·id_token으로 교환한다.
     *
     * @throws SocialTokenInvalidException 코드가 만료됐거나 이미 사용된 경우
     */
    public AppleTokens exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = baseForm();
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");
        Map<String, Object> response = SocialProviderCalls.block(post(TOKEN_PATH, form).bodyToMono(MAP_TYPE), TOKEN_PATH);
        Object refreshToken = response.get("refresh_token");
        Object idToken = response.get("id_token");
        if (refreshToken == null || idToken == null) {
            log.warn("Apple 코드 교환 응답에 refresh_token 또는 id_token 없음");
            throw new SocialTokenInvalidException();
        }
        return new AppleTokens(refreshToken.toString(), idToken.toString());
    }

    /**
     * 회원 탈퇴 시 Apple refresh_token을 revoke해 앱과 Apple ID의 연결을 끊는다.
     */
    public void revoke(String appleRefreshToken) {
        MultiValueMap<String, String> form = baseForm();
        form.add("token", appleRefreshToken);
        form.add("token_type_hint", "refresh_token");
        ResponseEntity<Void> response = SocialProviderCalls.block(post(REVOKE_PATH, form).toBodilessEntity(), REVOKE_PATH);
        log.info("Apple 토큰 revoke 완료 — status: {}", response.getStatusCode());
    }

    /**
     * Apple 4xx 중 invalid_grant(코드·토큰 만료·재사용)만 사용자 쪽 문제로 보고, 그 밖의 4xx
     * (invalid_client 등)는 우리 client_secret·키 설정 문제이므로 서버 오류로 올린다. 5xx는 제공자 장애다.
     */
    private WebClient.ResponseSpec post(String path, MultiValueMap<String, String> form) {
        return webClient.post()
                .uri(path)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(MAP_TYPE)
                        .defaultIfEmpty(Map.of())
                        .flatMap(body -> Mono.error(toClientError(path, response.statusCode(), body.get("error")))))
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.warn("Apple 인증 서버 오류 — path: {}, status: {}", path, response.statusCode());
                    return Mono.error(new SocialProviderUnavailableException());
                });
    }

    private static RuntimeException toClientError(String path, HttpStatusCode status, Object error) {
        if (INVALID_GRANT.equals(error)) {
            log.warn("Apple 요청 거절(invalid_grant) — path: {}", path);
            return new SocialTokenInvalidException();
        }
        log.error("Apple 요청 설정 오류 — path: {}, status: {}, error: {}", path, status, error);
        return new IllegalStateException("Apple 인증 서버 요청 설정 오류: " + error);
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.bundleId());
        form.add("client_secret", createClientSecret());
        return form;
    }

    String createClientSecret() {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(properties.keyId()).and()
                .issuer(properties.teamId())
                .subject(properties.bundleId())
                .audience().add(BASE_URL).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(CLIENT_SECRET_TTL)))
                .signWith(requirePrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey requirePrivateKey() {
        if (privateKey == null) {
            throw new IllegalStateException("Apple 개인키(app.oauth2.apple.private-key)가 설정되지 않았습니다.");
        }
        return privateKey;
    }

    /**
     * .p8 PEM 문자열(환경변수에서 줄바꿈이 "\n" 문자로 들어온 경우 포함)을 EC 개인키로 변환한다.
     */
    private static PrivateKey parsePrivateKey(String pem) {
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replace("\\n", "").replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Apple 개인키(app.oauth2.apple.private-key) 형식이 올바르지 않습니다.", e);
        }
    }
}
