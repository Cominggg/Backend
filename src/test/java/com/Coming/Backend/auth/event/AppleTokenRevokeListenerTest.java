package com.Coming.Backend.auth.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.service.SocialUserService;
import com.Coming.Backend.auth.social.AppleAuthClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppleTokenRevokeListenerTest {

    @InjectMocks
    private AppleTokenRevokeListener appleTokenRevokeListener;

    @Mock
    private AppleAuthClient appleAuthClient;

    @Mock
    private SocialUserService socialUserService;

    private static final Long USER_ID = 1L;
    private static final String APPLE_REFRESH_TOKEN = "apple-refresh-token";

    @Test
    void should_clear_apple_refresh_token_when_revoke_succeeds() {
        // given
        AppleTokenRevokeEvent event = new AppleTokenRevokeEvent(USER_ID, APPLE_REFRESH_TOKEN);

        // when
        appleTokenRevokeListener.onAppleTokenRevoke(event);

        // then
        verify(appleAuthClient).revoke(APPLE_REFRESH_TOKEN);
        verify(socialUserService).clearAppleRefreshToken(USER_ID);
    }

    @Test
    void should_keep_token_and_swallow_exception_when_revoke_fails() {
        // given
        AppleTokenRevokeEvent event = new AppleTokenRevokeEvent(USER_ID, APPLE_REFRESH_TOKEN);
        willThrow(new SocialProviderUnavailableException()).given(appleAuthClient).revoke(APPLE_REFRESH_TOKEN);

        // when & then
        assertThatCode(() -> appleTokenRevokeListener.onAppleTokenRevoke(event)).doesNotThrowAnyException();
        verify(socialUserService, never()).clearAppleRefreshToken(anyLong());
    }
}
