package com.Coming.Backend.concert.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.dto.ConcertDetailResponse;
import com.Coming.Backend.concert.dto.ConcertStatsResponse;
import com.Coming.Backend.concert.dto.ConcertSummaryResponse;
import com.Coming.Backend.concert.dto.SetlistResponse;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.service.ConcertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Concert")
@Validated
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @Operation(summary = "공연 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<ConcertSummaryResponse>> getConcerts(
            @RequestParam(required = false) ConcertStatus status,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(concertService.getConcerts(status, pageable, userId));
    }

    @Operation(summary = "인기 공연 목록 조회")
    @GetMapping("/popular")
    public ResponseEntity<List<ConcertSummaryResponse>> getPopularConcerts(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(concertService.getPopularConcerts(userId));
    }

    @Operation(summary = "월별 공연 통계 조회")
    @GetMapping("/stats")
    public ResponseEntity<ConcertStatsResponse> getConcertStats(
            @RequestParam int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(concertService.getConcertStats(year, month));
    }

    @Operation(summary = "관심 아티스트 공연 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/following")
    public ResponseEntity<List<ConcertSummaryResponse>> getFollowingConcerts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) ConcertStatus status) {
        return ResponseEntity.ok(concertService.getFollowingConcerts(userId, status));
    }

    @Operation(summary = "공연 상세 조회")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<ConcertDetailResponse> getConcert(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(concertService.getConcert(id, userId));
    }

    @Operation(summary = "셋리스트 조회")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @GetMapping("/{id}/setlist")
    public ResponseEntity<SetlistResponse> getSetlist(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getSetlist(id));
    }
}
