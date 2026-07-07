package com.Coming.Backend.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoogleOAuth2UserInfoTest {

    @Test
    void should_return_correct_values_when_full_attributes_given() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-sub-123");
        attributes.put("email", "test@gmail.com");

        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

        // when & then
        assertThat(userInfo.getProviderId()).isEqualTo("google-sub-123");
        assertThat(userInfo.getEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    void should_return_null_email_when_email_key_absent() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-sub-123");

        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

        // when & then
        assertThat(userInfo.getEmail()).isNull();
    }
}
