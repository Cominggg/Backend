package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Sign in with Apple identityToken을 검증한다. aud는 iOS 앱 번들 ID이고,
 * 앱은 서버가 발급한 nonce로 로그인을 요청해야 한다.
 */
@Component
public class AppleIdentityTokenVerifier implements SocialTokenVerifier {

    static final String JWK_SET_URI = "https://appleid.apple.com/auth/keys";
    static final String ISSUER = "https://appleid.apple.com";
    private static final String PROVIDER = "apple";

    private final JwtDecoder jwtDecoder;
    private final LoginNonceRepository loginNonceRepository;

    @Autowired
    public AppleIdentityTokenVerifier(
            @Value("${app.oauth2.apple.bundle-id}") String bundleId,
            LoginNonceRepository loginNonceRepository) {
        this(IdTokenDecoders.create(JWK_SET_URI, Set.of(ISSUER), bundleId), loginNonceRepository);
    }

    AppleIdentityTokenVerifier(JwtDecoder jwtDecoder, LoginNonceRepository loginNonceRepository) {
        this.jwtDecoder = jwtDecoder;
        this.loginNonceRepository = loginNonceRepository;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public OAuth2UserInfo verify(String token) {
        Jwt jwt = IdTokenDecoders.decode(jwtDecoder, token, PROVIDER);
        IdTokenDecoders.consumeNonce(jwt, loginNonceRepository, PROVIDER);
        return new AppleUserInfo(jwt.getClaims());
    }

    /**
     * authorizationCode 교환 응답의 id_token을 검증하고 sub를 꺼낸다. 이 토큰에는 nonce를 요구하지 않는다.
     */
    public String extractSubject(String idToken) {
        return IdTokenDecoders.decode(jwtDecoder, idToken, PROVIDER).getSubject();
    }
}
