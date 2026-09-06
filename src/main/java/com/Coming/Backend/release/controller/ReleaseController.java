package com.Coming.Backend.release.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.release.dto.ReleaseDetailResponse;
import com.Coming.Backend.release.dto.ReleaseListItemResponse;
import com.Coming.Backend.release.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Release")
@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @Operation(summary = "릴리즈 목록 조회")
    @ApiResponse(responseCode = "400", description = "INVALID_INPUT (type이 Album·Single이 아님)")
    @GetMapping
    public ResponseEntity<PageResponse<ReleaseListItemResponse>> getReleases(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean following,
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(releaseService.getReleases(q, artistId, type, userId, Boolean.TRUE.equals(following), pageable));
    }

    @Operation(summary = "릴리즈 상세 조회")
    @ApiResponse(responseCode = "404", description = "RELEASE_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<ReleaseDetailResponse> getReleaseDetail(@PathVariable Long id) {
        return ResponseEntity.ok(releaseService.getReleaseDetail(id));
    }
}
