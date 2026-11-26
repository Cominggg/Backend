package com.Coming.Backend.app.service;

import com.Coming.Backend.app.dto.AppVersionResponse;
import com.Coming.Backend.app.entity.AppPlatform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppVersionService {

    private final int androidMinVersionCode;
    private final int iosMinVersionCode;

    public AppVersionService(
            @Value("${app.min-version-code.android}") int androidMinVersionCode,
            @Value("${app.min-version-code.ios}") int iosMinVersionCode) {
        this.androidMinVersionCode = androidMinVersionCode;
        this.iosMinVersionCode = iosMinVersionCode;
    }

    /**
     * 플랫폼별 최소 지원 versionCode를 조회한다. 앱은 설치된 versionCode가 이보다 낮으면 강제 업데이트 화면을 띄운다.
     */
    public AppVersionResponse getMinVersion(AppPlatform platform) {
        int minVersionCode = switch (platform) {
            case ANDROID -> androidMinVersionCode;
            case IOS -> iosMinVersionCode;
        };
        return new AppVersionResponse(minVersionCode);
    }
}
