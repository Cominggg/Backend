package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KakaoOAuth2UserInfoTest {

    @Test
    void should_return_correct_values_when_full_attributes_given() {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456789L);
        attributes.put("kakao_account", kakaoAccount);

        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

        // when & then
        assertThat(userInfo.getProviderId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
    }

    @Test
    void should_return_null_email_when_kakao_account_key_absent() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456789L);

        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

        // when & then
        assertThat(userInfo.getEmail()).isNull();
    }

    @Test
    void should_return_null_email_when_email_key_absent() {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456789L);
        attributes.put("kakao_account", kakaoAccount);

        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

        // when & then
        assertThat(userInfo.getEmail()).isNull();
    }
}
