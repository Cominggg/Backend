package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.oauth2.GoogleOAuth2UserInfo;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.auth.service.SocialUserService.SocialUser;
import com.Coming.Backend.auth.social.AppleUserInfo;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * 동시 최초 로그인 경합을 실제 DB로 재현한다. 첫 조회만 비어 있게 만들어
 * 이미 커밋된 회원이 있는 상태에서 INSERT가 유니크 제약에 걸리도록 한다.
 */
@SpringBootTest
class SocialUserServiceConcurrencyTest {

    @Autowired
    private SocialUserService socialUserService;

    @MockitoSpyBean
    private UserRepository userRepository;

    private final String providerId = "race-" + UUID.randomUUID();

    @AfterEach
    void tearDown() {
        userRepository.findByProviderAndProviderId("google", providerId).ifPresent(userRepository::delete);
        userRepository.findByProviderAndProviderId("apple", providerId).ifPresent(userRepository::delete);
    }

    private User saveExistingUser(String provider) {
        return userRepository.save(User.builder()
                .email("winner@example.com")
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .build());
    }

    /** 첫 조회는 비어 있고, 재조회부터는 실제 DB를 조회한다. */
    private void stubFirstLookupEmpty(String provider, User winner) {
        doReturn(Optional.empty())
                .doAnswer(invocation -> userRepository.findById(winner.getId()))
                .when(userRepository).findByProviderAndProviderId(provider, providerId);
    }

    @Test
    void should_return_existing_user_when_concurrent_first_login_hits_unique_constraint() {
        // given
        User winner = saveExistingUser("google");
        stubFirstLookupEmpty("google", winner);

        // when
        SocialUser result = socialUserService.findOrCreate("google",
                new GoogleOAuth2UserInfo(Map.of("sub", providerId, "email", "loser@example.com")));

        // then
        assertThat(result.user().getId()).isEqualTo(winner.getId());
        assertThat(result.isNewUser()).isFalse();
    }

    @Test
    void should_save_apple_refresh_token_on_existing_user_when_concurrent_first_apple_login() {
        // given
        User winner = saveExistingUser("apple");
        stubFirstLookupEmpty("apple", winner);

        // when
        socialUserService.findOrCreateAppleUser(new AppleUserInfo(Map.of("sub", providerId)), "apple-refresh");

        // then
        assertThat(userRepository.findById(winner.getId()).orElseThrow().getAppleRefreshToken())
                .isEqualTo("apple-refresh");
    }

    @Test
    void should_persist_new_user_with_apple_refresh_token_when_apple_user_not_found() {
        // when
        SocialUser result = socialUserService.findOrCreateAppleUser(
                new AppleUserInfo(Map.of("sub", providerId)), "apple-refresh");

        // then
        assertThat(result.isNewUser()).isTrue();
        assertThat(userRepository.findById(result.user().getId()).orElseThrow().getAppleRefreshToken())
                .isEqualTo("apple-refresh");
    }

    @Test
    void should_rethrow_when_insert_violates_other_constraint() {
        // given: provider_id NOT NULL 위반
        // when & then
        assertThatThrownBy(() -> socialUserService.findOrCreate("google",
                new GoogleOAuth2UserInfo(Map.of("email", "no-sub@example.com"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
