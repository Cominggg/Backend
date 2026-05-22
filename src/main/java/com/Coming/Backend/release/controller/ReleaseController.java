package com.Coming.Backend.release.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.release.dto.ArtistReleaseItemResponse;
import com.Coming.Backend.release.dto.ReleaseDetailResponse;
import com.Coming.Backend.release.dto.ReleaseListItemResponse;
import com.Coming.Backend.release.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Release")
@RestController
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @Operation(summary = "아티스트 디스코그래피 조회")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND")
    @GetMapping("/api/artists/{id}/releases")
    public ResponseEntity<PageResponse<ArtistReleaseItemResponse>> getArtistReleases(
            @PathVariable Long id,
            @RequestParam(required = false) List<String> type,
            @PageableDefault(size = 10, sort = "firstReleaseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        List<String> types = type != null ? type : List.of();
        return ResponseEntity.ok(releaseService.getArtistReleases(id, types, pageable));
    }

    @Operation(summary = "전체 릴리즈 목록 조회")
    @GetMapping("/api/releases")
    public ResponseEntity<PageResponse<ReleaseListItemResponse>> getReleases(
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "firstReleaseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(releaseService.getReleases(artistId, type, pageable));
    }

    @Operation(summary = "릴리즈 상세 조회")
    @ApiResponse(responseCode = "404", description = "RELEASE_NOT_FOUND")
    @GetMapping("/api/releases/{id}")
    public ResponseEntity<ReleaseDetailResponse> getReleaseDetail(@PathVariable Long id) {
        return ResponseEntity.ok(releaseService.getReleaseDetail(id));
    }
}
