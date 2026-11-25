package com.Coming.Backend.app.controller;

import com.Coming.Backend.app.dto.AppVersionResponse;
import com.Coming.Backend.app.entity.AppPlatform;
import com.Coming.Backend.app.service.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;

    @Operation(summary = "앱 최소 지원 버전 조회")
    @ApiResponse(responseCode = "400", description = "INVALID_INPUT")
    @GetMapping("/version")
    public ResponseEntity<AppVersionResponse> getMinVersion(@RequestParam AppPlatform platform) {
        return ResponseEntity.ok(appVersionService.getMinVersion(platform));
    }
}
