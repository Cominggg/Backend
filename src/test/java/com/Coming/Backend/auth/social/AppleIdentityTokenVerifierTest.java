package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class AppleIdentityTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private LoginNonceRepository loginNonceRepository;

    private AppleIdentityTokenVerifier verifier;

    private static final String IDENTITY_TOKEN = "apple-identity-token";
    private static final String CODE_ID_TOKEN = "apple-code-id-token";
    private static final String PROVIDER_ID = "001234.abcdef0123456789.0123";
    private static final String EMAIL = "abc123@privaterelay.appleid.com";
    private static final String NONCE = "server-issued-nonce";

    @BeforeEach
    void setUp() {
        verifier = new AppleIdentityTokenVerifier(jwtDecoder, loginNonceRepository);
    }

    private Jwt.Builder appleJwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject(PROVIDER_ID);
    }

    // -------------------------------------------------------------------------
    // verify
    // -------------------------------------------------------------------------

    @Test
    void should_return_apple_as_provider() {
        // when & then
        assertThat(verifier.provider()).isEqualTo("apple");
    }

    @Test
    void should_return_user_info_and_consume_nonce_when_token_is_valid() {
        // given
        given(jwtDecoder.decode(IDENTITY_TOKEN))
                .willReturn(appleJwt(IDENTITY_TOKEN).claim("email", EMAIL).claim("nonce", NONCE).build());
        given(loginNonceRepository.consume(NONCE)).willReturn(true);

        // when
        OAuth2UserInfo userInfo = verifier.verify(IDENTITY_TOKEN);

        // then
        assertThat(userInfo).isInstanceOf(AppleUserInfo.class);
        assertThat(userInfo.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(userInfo.getEmail()).isEqualTo(EMAIL);
        verify(loginNonceRepository).consume(NONCE);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_nonce_claim_is_missing() {
        // given
        given(jwtDecoder.decode(IDENTITY_TOKEN)).willReturn(appleJwt(IDENTITY_TOKEN).build());

        // when & then
        assertThatThrownBy(() -> verifier.verify(IDENTITY_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_INVALID.getMessage());
        verify(loginNonceRepository, never()).consume(anyString());
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_identity_token_is_invalid() {
        // given
        given(jwtDecoder.decode(IDENTITY_TOKEN)).willThrow(new BadJwtException("Signed JWT rejected"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(IDENTITY_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class);
    }

    // -------------------------------------------------------------------------
    // extractSubject
    // -------------------------------------------------------------------------

    @Test
    void should_return_subject_without_checking_nonce_when_extract_subject() {
        // given
        given(jwtDecoder.decode(CODE_ID_TOKEN)).willReturn(appleJwt(CODE_ID_TOKEN).build());

        // when
        String subject = verifier.extractSubject(CODE_ID_TOKEN);

        // then
        assertThat(subject).isEqualTo(PROVIDER_ID);
        verifyNoInteractions(loginNonceRepository);
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_extract_subject_from_invalid_token() {
        // given
        given(jwtDecoder.decode(CODE_ID_TOKEN)).willThrow(new BadJwtException("Signed JWT rejected"));

        // when & then
        assertThatThrownBy(() -> verifier.extractSubject(CODE_ID_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class);
    }
}
