package com.Coming.Backend.auth.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.exception.SocialProviderUnavailableException;
import com.Coming.Backend.auth.social.KakaoUnlinkClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KakaoUnlinkListenerTest {

    @InjectMocks
    private KakaoUnlinkListener kakaoUnlinkListener;

    @Mock
    private KakaoUnlinkClient kakaoUnlinkClient;

    private static final Long USER_ID = 1L;
    private static final String KAKAO_USER_ID = "99999";

    @Test
    void should_request_unlink_with_kakao_user_id_when_event_received() {
        // given
        KakaoUnlinkEvent event = new KakaoUnlinkEvent(USER_ID, KAKAO_USER_ID);

        // when
        kakaoUnlinkListener.onKakaoUnlink(event);

        // then
        verify(kakaoUnlinkClient).unlink(KAKAO_USER_ID);
    }

    @Test
    void should_swallow_exception_when_unlink_fails() {
        // given
        KakaoUnlinkEvent event = new KakaoUnlinkEvent(USER_ID, KAKAO_USER_ID);
        willThrow(new SocialProviderUnavailableException()).given(kakaoUnlinkClient).unlink(KAKAO_USER_ID);

        // when & then
        assertThatCode(() -> kakaoUnlinkListener.onKakaoUnlink(event)).doesNotThrowAnyException();
    }
}
