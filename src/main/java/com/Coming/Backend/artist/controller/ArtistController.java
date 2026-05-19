package com.Coming.Backend.artist.controller;

import com.Coming.Backend.artist.dto.ArtistConcertResponse;
import com.Coming.Backend.artist.dto.ArtistDetailResponse;
import com.Coming.Backend.artist.dto.ArtistSummaryResponse;
import com.Coming.Backend.artist.dto.FollowingArtistResponse;
import com.Coming.Backend.artist.service.ArtistService;
import com.Coming.Backend.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Artist")
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @Operation(summary = "아티스트 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<ArtistSummaryResponse>> getArtists(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 25) Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(artistService.getArtists(name, pageable, userId));
    }

    @Operation(summary = "아티스트 상세 조회")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<ArtistDetailResponse> getArtist(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(artistService.getArtist(id, userId));
    }

    @Operation(summary = "아티스트 공연 내역 조회")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND")
    @GetMapping("/{id}/concerts")
    public ResponseEntity<PageResponse<ArtistConcertResponse>> getArtistConcerts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "all") String tab,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(artistService.getArtistConcerts(id, tab, pageable));
    }

    @Operation(summary = "아티스트 팔로우")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "ALREADY_FOLLOWING")
    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        artistService.follow(userId, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "아티스트 팔로우 취소")
    @ApiResponse(responseCode = "400", description = "NOT_FOLLOWING")
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        artistService.unfollow(userId, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "팔로잉 아티스트 목록 조회")
    @GetMapping("/following")
    public ResponseEntity<List<FollowingArtistResponse>> getFollowingArtists(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(artistService.getFollowingArtists(userId));
    }
}
