package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신규 소셜 회원 INSERT를 별도 트랜잭션으로 분리한다.
 * 동시 최초 로그인에서 유니크 제약 위반이 나도 이 트랜잭션만 롤백되어, 호출 측 트랜잭션에서 기존 회원을 재조회할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class SocialUserCreator {

    private final UserRepository userRepository;

    /**
     * PENDING·ACTIVE 상태의 신규 회원을 저장하고 즉시 커밋한다.
     *
     * @return 생성된 회원의 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long create(String provider, OAuth2UserInfo userInfo) {
        return userRepository.saveAndFlush(User.builder()
                .email(userInfo.getEmail())
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .build()).getId();
    }
}
