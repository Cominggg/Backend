package com.Coming.Backend.auth.social;

import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.oauth2.KakaoOAuth2UserInfo;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Kakao Access Token을 검증한다. 토큰 정보 API로 우리 앱(app_id)에서 발급된 토큰인지 확인한 뒤
 * 사용자 정보 API로 회원 식별 정보를 조회한다.
 */
@Slf4j
@Component
public class KakaoTokenVerifier implements SocialTokenVerifier {

    static final String BASE_URL = "https://kapi.kakao.com";
    private static final String PROVIDER = "kakao";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final String appId;

    @Autowired
    public KakaoTokenVerifier(WebClient.Builder builder, @Value("${app.oauth2.kakao.app-id}") String appId) {
        this(builder.baseUrl(BASE_URL).build(), appId);
    }

    KakaoTokenVerifier(WebClient webClient, String appId) {
        this.webClient = webClient;
        this.appId = appId;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public OAuth2UserInfo verify(String token) {
        Map<String, Object> tokenInfo = get("/v1/user/access_token_info", token);
        if (appId.isBlank() || !appId.equals(String.valueOf(tokenInfo.get("app_id")))) {
            log.warn("Kakao Access Token app_id 불일치 — app_id: {}", tokenInfo.get("app_id"));
            throw new SocialTokenInvalidException();
        }
        return new KakaoOAuth2UserInfo(get("/v2/user/me", token));
    }

    private Map<String, Object> get(String path, String token) {
        WebClient.ResponseSpec response = webClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve();
        return SocialProviderCalls.block(SocialProviderCalls.mapStatus(response, path).bodyToMono(MAP_TYPE), path);
    }
}
