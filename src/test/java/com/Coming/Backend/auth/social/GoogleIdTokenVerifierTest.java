package com.Coming.Backend.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.oauth2.GoogleOAuth2UserInfo;
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
class GoogleIdTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private LoginNonceRepository loginNonceRepository;

    private GoogleIdTokenVerifier verifier;

    private static final String ID_TOKEN = "google-id-token";
    private static final String PROVIDER_ID = "google-provider-id-001";
    private static final String EMAIL = "iu@gmail.com";
    private static final String NONCE = "server-issued-nonce";

    @BeforeEach
    void setUp() {
        verifier = new GoogleIdTokenVerifier(jwtDecoder, loginNonceRepository);
    }

    private Jwt.Builder googleJwt() {
        return Jwt.withTokenValue(ID_TOKEN)
                .header("alg", "RS256")
                .subject(PROVIDER_ID)
                .claim("email", EMAIL)
                .claim("nonce", NONCE);
    }

    @Test
    void should_return_google_as_provider() {
        // when & then
        assertThat(verifier.provider()).isEqualTo("google");
    }

    @Test
    void should_return_user_info_with_email_and_consume_nonce_when_email_is_verified() {
        // given
        given(jwtDecoder.decode(ID_TOKEN)).willReturn(googleJwt().claim("email_verified", true).build());
        given(loginNonceRepository.consume(NONCE)).willReturn(true);

        // when
        OAuth2UserInfo userInfo = verifier.verify(ID_TOKEN);

        // then
        assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        assertThat(userInfo.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(userInfo.getEmail()).isEqualTo(EMAIL);
        verify(loginNonceRepository).consume(NONCE);
    }

    @Test
    void should_drop_email_when_email_verified_is_false() {
        // given
        given(jwtDecoder.decode(ID_TOKEN)).willReturn(googleJwt().claim("email_verified", false).build());
        given(loginNonceRepository.consume(NONCE)).willReturn(true);

        // when
        OAuth2UserInfo userInfo = verifier.verify(ID_TOKEN);

        // then
        assertThat(userInfo.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(userInfo.getEmail()).isNull();
    }

    @Test
    void should_drop_email_when_email_verified_claim_is_missing() {
        // given
        given(jwtDecoder.decode(ID_TOKEN)).willReturn(googleJwt().build());
        given(loginNonceRepository.consume(NONCE)).willReturn(true);

        // when
        OAuth2UserInfo userInfo = verifier.verify(ID_TOKEN);

        // then
        assertThat(userInfo.getEmail()).isNull();
    }

    @Test
    void should_throw_SocialTokenInvalidException_when_nonce_is_already_used() {
        // given
        given(jwtDecoder.decode(ID_TOKEN)).willReturn(googleJwt().claim("email_verified", true).build());
        given(loginNonceRepository.consume(NONCE)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> verifier.verify(ID_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class)
                .hasMessage(ErrorCode.SOCIAL_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_SocialTokenInvalidException_without_consuming_nonce_when_token_is_invalid() {
        // given
        given(jwtDecoder.decode(ID_TOKEN)).willThrow(new BadJwtException("Signed JWT rejected"));

        // when & then
        assertThatThrownBy(() -> verifier.verify(ID_TOKEN))
                .isInstanceOf(SocialTokenInvalidException.class);
        verify(loginNonceRepository, never()).consume(anyString());
    }
}
