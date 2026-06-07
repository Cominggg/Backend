package com.Coming.Backend.auth.dev;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final JwtProvider jwtProvider;
    private final TokenRepository tokenRepository;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

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

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        tokenRepository.save(user.getId(), refreshToken, refreshTokenExpiry);

        log.info("Dev 로그인 — userId: {}, nickname: {}", user.getId(), user.getNickname());
        return new DevTokenResult(accessToken, refreshToken);
    }

    record DevTokenResult(String accessToken, String refreshToken) {
    }
}
