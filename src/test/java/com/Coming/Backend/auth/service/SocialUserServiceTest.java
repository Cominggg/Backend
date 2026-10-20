package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.oauth2.GoogleOAuth2UserInfo;
import com.Coming.Backend.auth.oauth2.KakaoOAuth2UserInfo;
import com.Coming.Backend.auth.oauth2.OAuth2UserInfo;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.auth.service.SocialUserService.SocialUser;
import com.Coming.Backend.auth.social.AppleUserInfo;
import com.Coming.Backend.common.exception.ErrorCode;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SocialUserServiceTest {

    @InjectMocks
    private SocialUserService socialUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialUserCreator socialUserCreator;

    private static final String GOOGLE = "google";
    private static final String GOOGLE_PROVIDER_ID = "google-provider-id-001";
    private static final String GOOGLE_EMAIL = "iu@gmail.com";
    private static final String KAKAO = "kakao";
    private static final String KAKAO_PROVIDER_ID = "99999";
    private static final String KAKAO_EMAIL = "iu@kakao.com";

    private OAuth2UserInfo googleUserInfo() {
        return new GoogleOAuth2UserInfo(Map.of("sub", GOOGLE_PROVIDER_ID, "email", GOOGLE_EMAIL));
    }

    private OAuth2UserInfo kakaoUserInfo() {
        return new KakaoOAuth2UserInfo(Map.of(
                "id", 99999L,
                "kakao_account", Map.of("email", KAKAO_EMAIL)));
    }

    private User buildGoogleUser(UserStatus status) {
        return User.builder()
                .email("old@gmail.com")
                .provider(GOOGLE)
                .providerId(GOOGLE_PROVIDER_ID)
                .role(UserRole.USER)
                .status(status)
                .nickname("IU")
                .birthYear(1993)
                .build();
    }

    @Test
    void should_return_existing_user_with_updated_email_and_isNewUser_false_when_active_user_exists() {
        // given
        User existingUser = buildGoogleUser(UserStatus.ACTIVE);
        given(userRepository.findByProviderAndProviderId(GOOGLE, GOOGLE_PROVIDER_ID))
                .willReturn(Optional.of(existingUser));

        // when
        SocialUser result = socialUserService.findOrCreate(GOOGLE, googleUserInfo());

        // then
        assertThat(result.user()).isSameAs(existingUser);
        assertThat(result.isNewUser()).isFalse();
        assertThat(existingUser.getEmail()).isEqualTo(GOOGLE_EMAIL);
        assertThat(existingUser.getRole()).isEqualTo(UserRole.USER);
        verify(socialUserCreator, never()).create(any(), any());
    }

    @Test
    void should_reactivate_user_with_updated_email_and_isNewUser_true_when_inactive_user_exists() {
        // given
        User inactiveUser = buildGoogleUser(UserStatus.INACTIVE);
        given(userRepository.findByProviderAndProviderId(GOOGLE, GOOGLE_PROVIDER_ID))
                .willReturn(Optional.of(inactiveUser));

        // when
        SocialUser result = socialUserService.findOrCreate(GOOGLE, googleUserInfo());

        // then
        assertThat(result.user()).isSameAs(inactiveUser);
        assertThat(result.isNewUser()).isTrue();
        assertThat(inactiveUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(inactiveUser.getRole()).isEqualTo(UserRole.PENDING);
        assertThat(inactiveUser.getNickname()).isNull();
        assertThat(inactiveUser.getBirthYear()).isNull();
        assertThat(inactiveUser.getEmail()).isEqualTo(GOOGLE_EMAIL);
        verify(socialUserCreator, never()).create(any(), any());
    }

    @Test
    void should_throw_UserSuspendedException_when_suspended_user_exists() {
        // given
        User suspendedUser = buildGoogleUser(UserStatus.SUSPENDED);
        given(userRepository.findByProviderAndProviderId(GOOGLE, GOOGLE_PROVIDER_ID))
                .willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> socialUserService.findOrCreate(GOOGLE, googleUserInfo()))
                .isInstanceOf(UserSuspendedException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    @Test
    void should_create_user_in_new_transaction_and_return_isNewUser_true_when_user_not_found() {
        // given
        User createdUser = User.builder()
                .id(10L)
                .email(KAKAO_EMAIL)
                .provider(KAKAO)
                .providerId(KAKAO_PROVIDER_ID)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .build();
        OAuth2UserInfo userInfo = kakaoUserInfo();
        given(userRepository.findByProviderAndProviderId(KAKAO, KAKAO_PROVIDER_ID))
                .willReturn(Optional.empty());
        given(socialUserCreator.create(KAKAO, userInfo)).willReturn(10L);
        given(userRepository.findById(10L)).willReturn(Optional.of(createdUser));

        // when
        SocialUser result = socialUserService.findOrCreate(KAKAO, userInfo);

        // then
        assertThat(result.user()).isSameAs(createdUser);
        assertThat(result.isNewUser()).isTrue();
    }

    @Test
    void should_return_refetched_user_and_isNewUser_false_when_create_fails_with_concurrent_duplicate_key() {
        // given
        User existingUser = User.builder()
                .provider(KAKAO)
                .providerId(KAKAO_PROVIDER_ID)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .build();
        given(userRepository.findByProviderAndProviderId(KAKAO, KAKAO_PROVIDER_ID))
                .willReturn(Optional.empty(), Optional.of(existingUser));
        given(socialUserCreator.create(any(), any()))
                .willThrow(constraintViolation("uq_user_provider_provider_id"));

        // when
        SocialUser result = socialUserService.findOrCreate(KAKAO, kakaoUserInfo());

        // then
        assertThat(result.user()).isSameAs(existingUser);
        assertThat(result.isNewUser()).isFalse();
    }

    @Test
    void should_rethrow_original_exception_when_create_fails_with_duplicate_key_and_user_still_not_found() {
        // given
        DataIntegrityViolationException original = constraintViolation("uq_user_provider_provider_id");
        given(userRepository.findByProviderAndProviderId(KAKAO, KAKAO_PROVIDER_ID))
                .willReturn(Optional.empty(), Optional.empty());
        given(socialUserCreator.create(any(), any())).willThrow(original);

        // when & then
        assertThatThrownBy(() -> socialUserService.findOrCreate(KAKAO, kakaoUserInfo()))
                .isSameAs(original);
    }

    @Test
    void should_rethrow_without_refetch_when_create_fails_with_other_constraint() {
        // given
        DataIntegrityViolationException original = constraintViolation("user_provider_id_not_null");
        given(userRepository.findByProviderAndProviderId(KAKAO, KAKAO_PROVIDER_ID))
                .willReturn(Optional.empty());
        given(socialUserCreator.create(any(), any())).willThrow(original);

        // when & then
        assertThatThrownBy(() -> socialUserService.findOrCreate(KAKAO, kakaoUserInfo()))
                .isSameAs(original);
        verify(userRepository, times(1)).findByProviderAndProviderId(KAKAO, KAKAO_PROVIDER_ID);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("constraint violation", new SQLException(), constraintName));
    }

    @Test
    void should_keep_existing_email_when_active_user_logs_in_without_email() {
        // given — 제공자가 email을 주지 않는 경우(예: Apple 재로그인)
        User existingUser = buildGoogleUser(UserStatus.ACTIVE);
        given(userRepository.findByProviderAndProviderId(GOOGLE, GOOGLE_PROVIDER_ID))
                .willReturn(Optional.of(existingUser));
        OAuth2UserInfo noEmailUserInfo = new GoogleOAuth2UserInfo(Map.of("sub", GOOGLE_PROVIDER_ID));

        // when
        SocialUser result = socialUserService.findOrCreate(GOOGLE, noEmailUserInfo);

        // then
        assertThat(result.isNewUser()).isFalse();
        assertThat(existingUser.getEmail()).isEqualTo("old@gmail.com");
    }

    @Test
    void should_keep_existing_email_when_inactive_user_rejoins_without_email() {
        // given
        User inactiveUser = buildGoogleUser(UserStatus.INACTIVE);
        given(userRepository.findByProviderAndProviderId(GOOGLE, GOOGLE_PROVIDER_ID))
                .willReturn(Optional.of(inactiveUser));
        OAuth2UserInfo noEmailUserInfo = new GoogleOAuth2UserInfo(Map.of("sub", GOOGLE_PROVIDER_ID));

        // when
        SocialUser result = socialUserService.findOrCreate(GOOGLE, noEmailUserInfo);

        // then
        assertThat(result.isNewUser()).isTrue();
        assertThat(inactiveUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(inactiveUser.getEmail()).isEqualTo("old@gmail.com");
    }

    // -------------------------------------------------------------------------
    // findOrCreateAppleUser
    // -------------------------------------------------------------------------

    private static final String APPLE_PROVIDER_ID = "001234.abcdef0123456789.0123";

    private User buildAppleUser(UserStatus status) {
        return User.builder()
                .id(1L)
                .provider("apple")
                .providerId(APPLE_PROVIDER_ID)
                .role(UserRole.USER)
                .status(status)
                .appleRefreshToken("old-apple-refresh-token")
                .build();
    }

    @Test
    void should_set_apple_refresh_token_on_existing_user_when_find_or_create_apple_user() {
        // given
        User existingUser = buildAppleUser(UserStatus.ACTIVE);
        given(userRepository.findByProviderAndProviderId("apple", APPLE_PROVIDER_ID))
                .willReturn(Optional.of(existingUser));

        // when
        SocialUser result = socialUserService.findOrCreateAppleUser(
                new AppleUserInfo(Map.of("sub", APPLE_PROVIDER_ID)), "new-apple-refresh-token");

        // then
        assertThat(result.user()).isSameAs(existingUser);
        assertThat(result.isNewUser()).isFalse();
        assertThat(existingUser.getAppleRefreshToken()).isEqualTo("new-apple-refresh-token");
    }

    @Test
    void should_create_apple_user_and_set_refresh_token_when_apple_user_not_found() {
        // given
        given(userRepository.findByProviderAndProviderId("apple", APPLE_PROVIDER_ID)).willReturn(Optional.empty());
        given(socialUserCreator.create(any(), any())).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(User.builder()
                .id(1L)
                .provider("apple")
                .providerId(APPLE_PROVIDER_ID)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .build()));

        // when
        SocialUser result = socialUserService.findOrCreateAppleUser(
                new AppleUserInfo(Map.of("sub", APPLE_PROVIDER_ID, "email", "abc123@privaterelay.appleid.com")),
                "new-apple-refresh-token");

        // then
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.user().getProvider()).isEqualTo("apple");
        assertThat(result.user().getRole()).isEqualTo(UserRole.PENDING);
        assertThat(result.user().getAppleRefreshToken()).isEqualTo("new-apple-refresh-token");
    }

    @Test
    void should_propagate_UserSuspendedException_and_keep_token_when_apple_user_is_suspended() {
        // given
        User suspendedUser = buildAppleUser(UserStatus.SUSPENDED);
        given(userRepository.findByProviderAndProviderId("apple", APPLE_PROVIDER_ID))
                .willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> socialUserService.findOrCreateAppleUser(
                new AppleUserInfo(Map.of("sub", APPLE_PROVIDER_ID)), "new-apple-refresh-token"))
                .isInstanceOf(UserSuspendedException.class);
        assertThat(suspendedUser.getAppleRefreshToken()).isEqualTo("old-apple-refresh-token");
    }

    // -------------------------------------------------------------------------
    // clearAppleRefreshToken
    // -------------------------------------------------------------------------

    @Test
    void should_clear_apple_refresh_token_when_user_exists() {
        // given
        User user = User.builder()
                .id(1L)
                .provider("apple")
                .providerId("001234.abcdef0123456789.0123")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .appleRefreshToken("revoked-apple-refresh-token")
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        socialUserService.clearAppleRefreshToken(1L);

        // then
        assertThat(user.getAppleRefreshToken()).isNull();
    }

    @Test
    void should_do_nothing_when_clearing_apple_refresh_token_for_missing_user() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatCode(() -> socialUserService.clearAppleRefreshToken(999L)).doesNotThrowAnyException();
    }
}
