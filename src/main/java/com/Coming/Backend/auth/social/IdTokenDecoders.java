package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

/**
 * 제공자 JWKS로 서명을 검증하고 만료·발급자·대상(aud)을 확인하는 ID Token 디코더를 만들고 쓴다.
 */
@Slf4j
final class IdTokenDecoders {

    private static final String NONCE_CLAIM = "nonce";
    private static final Duration JWKS_TIMEOUT = Duration.ofSeconds(5);

    private IdTokenDecoders() {
    }

    static JwtDecoder create(String jwkSetUri, Set<String> issuers, String audience) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(JWKS_TIMEOUT);
        requestFactory.setReadTimeout(JWKS_TIMEOUT);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(new RestTemplate(requestFactory))
                .build();
        decoder.setJwtValidator(validator(issuers, audience));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validator(Set<String> issuers, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        issuer -> issuer != null && issuers.contains(issuer.toString())),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        audiences -> audiences != null && !audience.isBlank() && audiences.contains(audience))
        );
    }

    /**
     * 토큰 자체가 잘못됐으면(서명·클레임 검증 실패) SOCIAL_TOKEN_INVALID, JWKS 조회 실패 등
     * 제공자 쪽 문제면 SOCIAL_PROVIDER_UNAVAILABLE로 바꾼다.
     */
    static Jwt decode(JwtDecoder decoder, String token, String provider) {
        try {
            return decoder.decode(token);
        } catch (BadJwtException e) {
            log.warn("{} ID Token 검증 실패 — {}", provider, e.getMessage());
            throw new SocialTokenInvalidException();
        } catch (JwtException e) {
            log.warn("{} ID Token 검증 중 제공자 키 조회 실패 — {}", provider, e.getMessage());
            throw new SocialProviderUnavailableException();
        }
    }

    /**
     * 토큰의 nonce가 서버가 발급한 미사용 nonce인지 확인하고 소비한다.
     */
    static void consumeNonce(Jwt jwt, LoginNonceRepository loginNonceRepository, String provider) {
        String nonce = jwt.getClaimAsString(NONCE_CLAIM);
        if (nonce == null || !loginNonceRepository.consume(nonce)) {
            log.warn("{} ID Token nonce 불일치 또는 재사용", provider);
            throw new SocialTokenInvalidException();
        }
    }
}
