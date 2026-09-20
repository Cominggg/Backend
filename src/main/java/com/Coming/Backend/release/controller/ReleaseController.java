package com.Coming.Backend.release.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.rating.dto.RatingMeResponse;
import com.Coming.Backend.rating.dto.RatingUpsertRequest;
import com.Coming.Backend.rating.entity.RatingTargetType;
import com.Coming.Backend.rating.service.RatingService;
import com.Coming.Backend.release.dto.ReleaseDetailResponse;
import com.Coming.Backend.release.dto.ReleaseListItemResponse;
import com.Coming.Backend.release.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Release")
@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;
    private final RatingService ratingService;

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

    @Operation(summary = "릴리즈 별점 등록·수정")
    @ApiResponse(responseCode = "400", description = "INVALID_RATING_SCORE")
    @ApiResponse(responseCode = "404", description = "RATING_TARGET_NOT_FOUND")
    @PutMapping("/{id}/rating")
    public ResponseEntity<Void> upsertRating(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RatingUpsertRequest request) {
        ratingService.upsert(userId, RatingTargetType.RELEASE, id, request.score());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 릴리즈 별점 조회")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    @GetMapping("/{id}/rating/me")
    public ResponseEntity<RatingMeResponse> getMyRating(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ratingService.getMine(userId, RatingTargetType.RELEASE, id));
    }

    @Operation(summary = "릴리즈 별점 취소")
    @ApiResponse(responseCode = "404", description = "RATING_NOT_FOUND")
    @DeleteMapping("/{id}/rating")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        ratingService.delete(userId, RatingTargetType.RELEASE, id);
        return ResponseEntity.ok().build();
    }
}
