package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@ExtendWith(MockitoExtension.class)
class IdTokenDecodersTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private LoginNonceRepository loginNonceRepository;

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String CLIENT_ID = "coming-web-client-id.apps.googleusercontent.com";
    private static final String ID_TOKEN = "id-token";
    private static final String NONCE = "server-issued-nonce";

    private Jwt jwt(String issuer, List<String> audience, Instant issuedAt, Instant expiresAt) {
        return Jwt.withTokenValue(ID_TOKEN)
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .subject("google-provider-id-001")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private Jwt validJwt(String issuer, List<String> audience) {
        Instant now = Instant.now();
        return jwt(issuer, audience, now, now.plusSeconds(3600));
    }

    private Jwt jwtWithNonce(String nonce) {
        Jwt.Builder builder = Jwt.withTokenValue(ID_TOKEN)
                .header("alg", "RS256")
                .subject("google-provider-id-001");
        if (nonce != null) {
            builder.claim("nonce", nonce);
        }
        return builder.build();
    }

    // -------------------------------------------------------------------------
    // validator
    // -------------------------------------------------------------------------

    @Test
    void should_pass_when_issuer_audience_and_expiry_are_valid() {
        // given
        OAuth2TokenValidator<Jwt> validator = IdTokenDecoders.validator(ISSUERS, CLIENT_ID);
        Jwt token = validJwt("https://accounts.google.com", List.of(CLIENT_ID));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void should_fail_when_issuer_does_not_match() {
        // given
        OAuth2TokenValidator<Jwt> validator = IdTokenDecoders.validator(ISSUERS, CLIENT_ID);
        Jwt token = validJwt("https://evil.example.com", List.of(CLIENT_ID));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void should_fail_when_audience_does_not_match() {
        // given
        OAuth2TokenValidator<Jwt> validator = IdTokenDecoders.validator(ISSUERS, CLIENT_ID);
        Jwt token = validJwt("https://accounts.google.com", List.of("other-app-client-id"));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void should_fail_when_configured_audience_is_blank() {
        // given — 설정값이 비어 있으면 aud에 빈 문자열이 들어 있어도 통과시키지 않는다
        OAuth2TokenValidator<Jwt> validator = IdTokenDecoders.validator(ISSUERS, "");
        Jwt token = validJwt("https://accounts.google.com", List.of(""));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void should_fail_when_token_is_expired() {
        // given
        OAuth2TokenValidator<Jwt> validator = IdTokenDecoders.validator(ISSUERS, CLIENT_ID);
        Instant now = Instant.now();
        Jwt token = jwt("https://accounts.google.com", List.of(CLIENT_ID),
                now.minusSeconds(7200), now.minusSeconds(3600));

        // when
        OAuth2TokenValidatorResult result = validator.validate(token);

        // then
        assertThat(result.hasErrors()).isTrue();
    }

    // -------------------------------------------------------------------------
    // decode
    // -------------------------------------------------------------------------

    @Test
    void should_return_decoded_jwt_when_decode_succeeds() {
        // given
        Jwt decoded = jwtWithNonce(NONCE);
        given(jwtDecoder.decode(ID_TOKEN)).willReturn(decoded);

        // when
        Jwt result = IdTokenDecoders.decode(jwtDecoder, ID_TOKEN, "google");

        // then
        assertThat(result).isSameAs(decoded);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_decoder_throws_BadJwtException() {
        // given
        given(jwtDecoder.decode(ID_TOKEN)).willThrow(new BadJwtException("Signed JWT rejected"));

        // when & then
        assertThatThrownBy(() -> IdTokenDecoders.decode(jwtDecoder, ID_TOKEN, "google"))
                .isInstanceOf(SocialTokenInvalidException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_SocialProviderUnavailableException_when_decoder_fails_to_fetch_jwk_set() {
        // given
        given(jwtDecoder.decode(ID_TOKEN))
                .willThrow(new JwtException("Couldn't retrieve remote JWK set: Read timed out"));

        // when & then
        assertThatThrownBy(() -> IdTokenDecoders.decode(jwtDecoder, ID_TOKEN, "google"))
                .isInstanceOf(SocialProviderUnavailableException.class)
                .hasMessage(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE.getMessage());
    }

    // -------------------------------------------------------------------------
    // consumeNonce
    // -------------------------------------------------------------------------

    @Test
    void should_consume_nonce_when_nonce_was_issued_by_server() {
        // given
        given(loginNonceRepository.consume(NONCE)).willReturn(true);

        // when
        IdTokenDecoders.consumeNonce(jwtWithNonce(NONCE), loginNonceRepository, "google");

        // then
        verify(loginNonceRepository).consume(NONCE);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_nonce_claim_is_missing() {
        // given
        Jwt token = jwtWithNonce(null);

        // when & then
        assertThatThrownBy(() -> IdTokenDecoders.consumeNonce(token, loginNonceRepository, "google"))
                .isInstanceOf(SocialTokenInvalidException.class);
        verify(loginNonceRepository, never()).consume(anyString());
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_nonce_is_unknown_or_already_used() {
        // given
        given(loginNonceRepository.consume(NONCE)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> IdTokenDecoders.consumeNonce(jwtWithNonce(NONCE), loginNonceRepository, "google"))
                .isInstanceOf(SocialTokenInvalidException.class);
    }
}
