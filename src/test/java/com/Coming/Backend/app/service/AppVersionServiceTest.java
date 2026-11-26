package com.Coming.Backend.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.app.dto.AppVersionResponse;
import com.Coming.Backend.app.entity.AppPlatform;
import org.junit.jupiter.api.Test;

class AppVersionServiceTest {

    private final AppVersionService appVersionService = new AppVersionService(10203, 500);

    @Test
    void should_return_android_min_version_code_when_platform_is_android() {
        // given
        AppPlatform platform = AppPlatform.ANDROID;

        // when
        AppVersionResponse response = appVersionService.getMinVersion(platform);

        // then
        assertThat(response.minVersionCode()).isEqualTo(10203);
    }

    @Test
    void should_return_ios_min_version_code_when_platform_is_ios() {
        // given
        AppPlatform platform = AppPlatform.IOS;

        // when
        AppVersionResponse response = appVersionService.getMinVersion(platform);

        // then
        assertThat(response.minVersionCode()).isEqualTo(500);
    }
}
