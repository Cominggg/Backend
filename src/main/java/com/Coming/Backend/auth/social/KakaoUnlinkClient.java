package com.Coming.Backend.auth.social;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 회원 탈퇴 시 Kakao 연결 끊기를 요청한다. 사용자 토큰 없이 앱 Admin 키로 회원번호를 지정해 호출한다.
 */
@Slf4j
@Component
public class KakaoUnlinkClient {

    static final String BASE_URL = "https://kapi.kakao.com";
    private static final String UNLINK_PATH = "/v1/user/unlink";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final String adminKey;

    @Autowired
    public KakaoUnlinkClient(WebClient.Builder builder, @Value("${app.oauth2.kakao.admin-key}") String adminKey) {
        this(builder.baseUrl(BASE_URL).build(), adminKey);
    }

    KakaoUnlinkClient(WebClient webClient, String adminKey) {
        this.webClient = webClient;
        this.adminKey = adminKey;
    }

    /**
     * @param kakaoUserId Kakao 회원번호 (탈퇴 전 provider_id)
     */
    public void unlink(String kakaoUserId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", kakaoUserId);
        WebClient.ResponseSpec response = webClient.post()
                .uri(UNLINK_PATH)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                .body(BodyInserters.fromFormData(form))
                .retrieve();
        SocialProviderCalls.block(SocialProviderCalls.mapStatus(response, UNLINK_PATH).bodyToMono(MAP_TYPE), UNLINK_PATH);
        log.info("Kakao 연결 끊기 완료");
    }
}
