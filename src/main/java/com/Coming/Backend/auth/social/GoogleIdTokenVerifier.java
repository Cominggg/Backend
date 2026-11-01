package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.oauth2.GoogleOAuth2UserInfo;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Google ID Token을 검증한다. 앱(Android·iOS)은 serverClientId로 웹 클라이언트 ID를 지정하므로
 * aud는 웹 로그인과 같은 클라이언트 ID다. 앱은 서버가 발급한 nonce로 토큰을 요청해야 한다.
 */
@Component
public class GoogleIdTokenVerifier implements SocialTokenVerifier {

    static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String PROVIDER = "google";

    private final JwtDecoder jwtDecoder;
    private final LoginNonceRepository loginNonceRepository;

    @Autowired
    public GoogleIdTokenVerifier(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            LoginNonceRepository loginNonceRepository) {
        this(IdTokenDecoders.create(JWK_SET_URI, ISSUERS, clientId), loginNonceRepository);
    }

    GoogleIdTokenVerifier(JwtDecoder jwtDecoder, LoginNonceRepository loginNonceRepository) {
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
        return new GoogleOAuth2UserInfo(withoutUnverifiedEmail(jwt.getClaims()));
    }

    /**
     * email_verified가 true가 아닌 이메일은 메일 발송에 쓰이지 않도록 저장하지 않는다.
     */
    private static Map<String, Object> withoutUnverifiedEmail(Map<String, Object> claims) {
        if (Boolean.TRUE.equals(claims.get("email_verified"))) {
            return claims;
        }
        Map<String, Object> filtered = new HashMap<>(claims);
        filtered.remove("email");
        return filtered;
    }
}
