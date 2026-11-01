package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.event.AppleTokenRevokeEvent;
import com.Coming.Backend.auth.event.KakaoUnlinkEvent;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import com.Coming.Backend.rating.repository.RatingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowArtistRepository userFollowArtistRepository;

    @Mock
    private UserConcertCalendarRepository userConcertCalendarRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long USER_ID = 1L;

    private User buildUser(String provider, String appleRefreshToken) {
        return User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .nickname("테스터")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .provider(provider)
                .providerId("provider-123")
                .appleRefreshToken(appleRefreshToken)
                .build();
    }

    @Test
    void should_delete_user_data_and_clear_personal_info_and_all_sessions_when_withdraw() {
        // given
        User user = buildUser("google", null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        withdrawalService.withdraw(USER_ID);

        // then
        verify(userFollowArtistRepository).deleteByUserId(USER_ID);
        verify(userConcertCalendarRepository).deleteByUserId(USER_ID);
        verify(inquiryRepository).deleteByUserId(USER_ID);
        verify(ratingRepository).deleteByUserId(USER_ID);
        verify(tokenRepository).deleteAllSessions(USER_ID);
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getProviderId()).isEqualTo("withdrawn:" + USER_ID);
        verify(eventPublisher, never()).publishEvent(any(AppleTokenRevokeEvent.class));
    }

    @Test
    void should_publish_apple_token_revoke_event_and_keep_token_when_apple_user_withdraws() {
        // given
        User user = buildUser("apple", "apple-refresh-token");
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        withdrawalService.withdraw(USER_ID);

        // then — revoke는 커밋 후 리스너가 처리하므로 토큰은 아직 지우지 않는다
        verify(eventPublisher).publishEvent(new AppleTokenRevokeEvent(USER_ID, "apple-refresh-token"));
        assertThat(user.getAppleRefreshToken()).isEqualTo("apple-refresh-token");
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void should_publish_kakao_unlink_event_with_original_provider_id_when_kakao_user_withdraws() {
        // given
        User user = buildUser("kakao", null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        withdrawalService.withdraw(USER_ID);

        // then
        verify(eventPublisher).publishEvent(new KakaoUnlinkEvent(USER_ID, "provider-123"));
        assertThat(user.getProviderId()).isEqualTo("withdrawn:" + USER_ID);
    }

    @Test
    void should_not_publish_kakao_unlink_event_when_non_kakao_user_withdraws() {
        // given
        User user = buildUser("google", null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        withdrawalService.withdraw(USER_ID);

        // then
        verify(eventPublisher, never()).publishEvent(any(KakaoUnlinkEvent.class));
    }

    @Test
    void should_throw_UserNotFoundException_and_not_publish_events_when_user_has_already_withdrawn() {
        // given
        User user = buildUser("kakao", null);
        user.withdraw();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any(KakaoUnlinkEvent.class));
        verify(tokenRepository, never()).deleteAllSessions(any());
    }

    @Test
    void should_throw_UserNotFoundException_and_keep_sessions_when_user_does_not_exist() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdraw(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
        verify(tokenRepository, never()).deleteAllSessions(any());
    }
}
