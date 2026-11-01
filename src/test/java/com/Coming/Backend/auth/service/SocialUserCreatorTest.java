package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.oauth2.KakaoOAuth2UserInfo;
import com.Coming.Backend.auth.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialUserCreatorTest {

    @InjectMocks
    private SocialUserCreator socialUserCreator;

    @Mock
    private UserRepository userRepository;

    @Test
    void should_save_and_flush_pending_active_user_and_return_id_when_creating() {
        // given
        given(userRepository.saveAndFlush(any(User.class))).willReturn(User.builder().id(10L).build());

        // when
        Long userId = socialUserCreator.create("kakao", new KakaoOAuth2UserInfo(Map.of(
                "id", 99999L,
                "kakao_account", Map.of("email", "iu@kakao.com"))));

        // then
        assertThat(userId).isEqualTo(10L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User toSave = userCaptor.getValue();
        assertThat(toSave.getProvider()).isEqualTo("kakao");
        assertThat(toSave.getProviderId()).isEqualTo("99999");
        assertThat(toSave.getEmail()).isEqualTo("iu@kakao.com");
        assertThat(toSave.getRole()).isEqualTo(UserRole.PENDING);
        assertThat(toSave.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(toSave.getNickname()).isNull();
    }
}
