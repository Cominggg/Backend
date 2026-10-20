package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUserService {

    private static final String APPLE = "apple";

    private static final String PROVIDER_ID_UNIQUE_CONSTRAINT = "uq_user_provider_provider_id";

    private final UserRepository userRepository;
    private final SocialUserCreator socialUserCreator;

    public record SocialUser(User user, boolean isNewUser) {}

    /**
     * 소셜 제공자 식별자로 회원을 조회하고, 없으면 PENDING 회원으로 생성한다.
     * 탈퇴(INACTIVE) 회원은 재가입 처리하고, 정지(SUSPENDED) 회원은 로그인을 막는다.
     *
     * @throws UserSuspendedException 정지된 계정인 경우
     */
    @Transactional
    public SocialUser findOrCreate(String provider, OAuth2UserInfo userInfo) {
        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId());
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getStatus() == UserStatus.SUSPENDED) {
                log.warn("정지된 계정 로그인 시도 — userId: {}", user.getId());
                throw new UserSuspendedException();
            }
            if (user.getStatus() == UserStatus.INACTIVE) {
                log.info("탈퇴 후 재가입 처리 — userId: {}", user.getId());
                user.reactivate();
                updateEmailIfPresent(user, userInfo);
                return new SocialUser(user, true);
            }
            updateEmailIfPresent(user, userInfo);
            return new SocialUser(user, false);
        }
        log.info("신규 소셜 로그인 사용자 생성 — provider: {}, providerId: {}", provider, userInfo.getProviderId());
        try {
            Long userId = socialUserCreator.create(provider, userInfo);
            return new SocialUser(userRepository.findById(userId).orElseThrow(), true);
        } catch (DataIntegrityViolationException e) {
            if (!isProviderIdDuplicate(e)) {
                throw e;
            }
            log.info("동시 최초 로그인 경합 — 기존 사용자 재조회. provider: {}, providerId: {}", provider, userInfo.getProviderId());
            User user = userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                    .orElseThrow(() -> e);
            return new SocialUser(user, false);
        }
    }

    /**
     * Apple 회원을 조회·생성하고, 탈퇴 시 revoke에 쓸 Apple refresh_token을 같은 트랜잭션에서 저장한다.
     * 로그인할 때마다 최신 값으로 덮어쓴다.
     *
     * @throws UserSuspendedException 정지된 계정인 경우
     */
    @Transactional
    public SocialUser findOrCreateAppleUser(OAuth2UserInfo userInfo, String appleRefreshToken) {
        SocialUser socialUser = findOrCreate(APPLE, userInfo);
        socialUser.user().updateAppleRefreshToken(appleRefreshToken);
        return socialUser;
    }

    /**
     * revoke가 끝난 Apple refresh_token을 지운다. 탈퇴 트랜잭션 커밋 뒤에 호출되므로 새 트랜잭션에서 실행한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearAppleRefreshToken(Long userId) {
        userRepository.findById(userId).ifPresent(user -> user.updateAppleRefreshToken(null));
    }

    private boolean isProviderIdDuplicate(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && PROVIDER_ID_UNIQUE_CONSTRAINT.equals(cve.getConstraintName());
    }

    /**
     * 제공자가 email을 주지 않으면(Apple 재로그인, Kakao 이메일 동의 철회, Google 미인증 이메일) 기존 값을 유지한다.
     */
    private void updateEmailIfPresent(User user, OAuth2UserInfo userInfo) {
        if (userInfo.getEmail() != null) {
            user.updateEmail(userInfo.getEmail());
        }
    }
}
