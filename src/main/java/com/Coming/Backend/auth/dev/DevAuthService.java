package com.Coming.Backend.auth.dev;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.dto.TokenPair;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Profile("local")
@Service
@RequiredArgsConstructor
public class DevAuthService {

    private static final String DEV_PROVIDER = "local";

    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * nickname 기준으로 로컬 유저를 upsert하고 토큰을 발급한다.
     */
    @Transactional
    public DevTokenResult login(String nickname, UserRole role) {
        User user = userRepository.findByProviderAndProviderId(DEV_PROVIDER, nickname)
                .orElseGet(() -> {
                    User created = User.builder()
                            .provider(DEV_PROVIDER)
                            .providerId(nickname)
                            .nickname(nickname)
                            .role(role)
                            .status(UserStatus.ACTIVE)
                            .build();
                    log.info("Dev 유저 생성 — nickname: {}, role: {}", nickname, role);
                    return userRepository.save(created);
                });

        TokenPair tokens = authService.issueTokens(user);

        log.info("Dev 로그인 — userId: {}, nickname: {}", user.getId(), user.getNickname());
        return new DevTokenResult(tokens.accessToken(), tokens.refreshToken());
    }

    record DevTokenResult(String accessToken, String refreshToken) {
    }
}
