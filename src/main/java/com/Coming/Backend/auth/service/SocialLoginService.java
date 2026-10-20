package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.dto.LoginNonceResponse;
import com.Coming.Backend.auth.dto.SocialLoginResponse;
import com.Coming.Backend.auth.dto.SocialTokenLoginRequest;
import com.Coming.Backend.auth.dto.TokenPair;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.exception.SocialTokenInvalidException;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.LoginNonceRepository;
import com.Coming.Backend.auth.service.SocialUserService.SocialUser;
import com.Coming.Backend.auth.social.AppleAuthClient;
import com.Coming.Backend.auth.social.AppleAuthClient.AppleTokens;
import com.Coming.Backend.auth.social.AppleIdentityTokenVerifier;
import com.Coming.Backend.auth.social.SocialTokenVerifier;
import com.Coming.Backend.common.exception.InvalidInputException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SocialLoginService {

    private static final Duration NONCE_TTL = Duration.ofMinutes(5);
    private static final int NONCE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, SocialTokenVerifier> verifiers;
    private final SocialUserService socialUserService;
    private final AuthService authService;
    private final AppleAuthClient appleAuthClient;
    private final AppleIdentityTokenVerifier appleIdentityTokenVerifier;
    private final LoginNonceRepository loginNonceRepository;

    public SocialLoginService(List<SocialTokenVerifier> verifiers, SocialUserService socialUserService,
            AuthService authService, AppleAuthClient appleAuthClient,
            AppleIdentityTokenVerifier appleIdentityTokenVerifier, LoginNonceRepository loginNonceRepository) {
        this.verifiers = verifiers.stream()
                .collect(Collectors.toMap(SocialTokenVerifier::provider, Function.identity()));
        this.socialUserService = socialUserService;
        this.authService = authService;
        this.appleAuthClient = appleAuthClient;
        this.appleIdentityTokenVerifier = appleIdentityTokenVerifier;
        this.loginNonceRepository = loginNonceRepository;
    }

    /**
     * Google·Apple SDK 로그인 요청에 넣을 1회용 nonce를 발급한다. 5분 안에 한 번만 쓸 수 있다.
     */
    public LoginNonceResponse issueNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        loginNonceRepository.save(nonce, NONCE_TTL);
        return new LoginNonceResponse(nonce);
    }

    /**
     * 앱이 소셜 SDK로 받은 토큰을 검증하고 회원을 조회·생성한 뒤 새 세션의 Access/Refresh Token을 발급한다.
     * 제공자 호출(검증·코드 교환)은 DB 트랜잭션 밖에서 먼저 끝낸다.
     *
     * @throws InvalidInputException 지원하지 않는 provider이거나 apple인데 authorizationCode가 없는 경우
     */
    public SocialLoginResponse login(String provider, SocialTokenLoginRequest request) {
        SocialTokenVerifier verifier = verifiers.get(provider);
        boolean isApple = appleIdentityTokenVerifier.provider().equals(provider);
        if (verifier == null || (isApple && !StringUtils.hasText(request.authorizationCode()))) {
            throw new InvalidInputException();
        }
        OAuth2UserInfo userInfo = verifier.verify(request.token());
        String appleRefreshToken = isApple ? exchangeAppleCode(request.authorizationCode(), userInfo) : null;

        SocialUser socialUser = isApple
                ? socialUserService.findOrCreateAppleUser(userInfo, appleRefreshToken)
                : socialUserService.findOrCreate(provider, userInfo);
        User user = socialUser.user();
        TokenPair tokens = authService.issueTokens(user);
        log.info("앱 소셜 로그인 성공 — provider: {}, userId: {}, isNewUser: {}", provider, user.getId(), socialUser.isNewUser());
        return new SocialLoginResponse(tokens.accessToken(), tokens.refreshToken(), user.getRole() == UserRole.PENDING);
    }

    /**
     * authorizationCode를 교환하고, 코드의 주인(id_token sub)이 로그인한 identityToken의 사용자와 같은지 확인한다.
     */
    private String exchangeAppleCode(String authorizationCode, OAuth2UserInfo userInfo) {
        AppleTokens appleTokens = appleAuthClient.exchangeAuthorizationCode(authorizationCode);
        String codeSubject = appleIdentityTokenVerifier.extractSubject(appleTokens.idToken());
        if (codeSubject == null || !codeSubject.equals(userInfo.getProviderId())) {
            log.warn("Apple authorizationCode와 identityToken의 사용자 불일치");
            throw new SocialTokenInvalidException();
        }
        return appleTokens.refreshToken();
    }
}
