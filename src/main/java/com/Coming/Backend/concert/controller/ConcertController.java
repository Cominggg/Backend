package com.Coming.Backend.concert.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.common.util.SortPropertyValidator;
import com.Coming.Backend.concert.dto.ConcertDetailResponse;
import com.Coming.Backend.concert.dto.ConcertStatsResponse;
import com.Coming.Backend.concert.dto.ConcertSummaryResponse;
import com.Coming.Backend.concert.dto.SetlistResponse;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.UnauthorizedException;
import com.Coming.Backend.concert.service.ConcertService;
import com.Coming.Backend.rating.dto.RatingMeResponse;
import com.Coming.Backend.rating.dto.RatingUpsertRequest;
import com.Coming.Backend.rating.entity.RatingTargetType;
import com.Coming.Backend.rating.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Tag(name = "Concert")
@Validated
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("startDate", "ticketOpenAt");

    private final ConcertService concertService;
    private final RatingService ratingService;

    @Operation(summary = "공연 목록 조회")
    @ApiResponse(responseCode = "400", description = "INVALID_INPUT (허용되지 않은 sort 필드)")
    @GetMapping
    public ResponseEntity<PageResponse<ConcertSummaryResponse>> getConcerts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ConcertStatus status,
            @RequestParam(required = false) Boolean inCalendar,
            @RequestParam(required = false) Boolean followedOnly,
            @RequestParam(required = false) Boolean ticketOpenPending,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        SortPropertyValidator.validate(pageable, SORTABLE_PROPERTIES);
        return ResponseEntity.ok(concertService.getConcerts(q, status, inCalendar, followedOnly, ticketOpenPending, pageable, userId));
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

    @Operation(summary = "공연 상세 조회")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<ConcertDetailResponse> getConcert(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(concertService.getConcert(id, userId));
    }

    @Operation(summary = "티켓 오픈 예정 공연 조회")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED (following=true이고 비인증)")
    @GetMapping("/ticketing")
    public ResponseEntity<List<ConcertSummaryResponse>> getTicketingConcerts(
            @RequestParam(defaultValue = "false") boolean following,
            @AuthenticationPrincipal Long userId) {
        if (following && userId == null) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(concertService.getTicketingConcerts(userId, following));
    }

    @Operation(summary = "셋리스트 조회")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @GetMapping("/{id}/setlist")
    public ResponseEntity<SetlistResponse> getSetlist(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getSetlist(id));
    }

    @Operation(summary = "공연 별점 등록·수정")
    @ApiResponse(responseCode = "400", description = "INVALID_RATING_SCORE")
    @ApiResponse(responseCode = "404", description = "RATING_TARGET_NOT_FOUND")
    @PutMapping("/{id}/rating")
    public ResponseEntity<Void> upsertRating(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RatingUpsertRequest request) {
        ratingService.upsert(userId, RatingTargetType.CONCERT, id, request.score());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 공연 별점 조회")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    @GetMapping("/{id}/rating/me")
    public ResponseEntity<RatingMeResponse> getMyRating(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ratingService.getMine(userId, RatingTargetType.CONCERT, id));
    }

    @Operation(summary = "공연 별점 취소")
    @ApiResponse(responseCode = "404", description = "RATING_NOT_FOUND")
    @DeleteMapping("/{id}/rating")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        ratingService.delete(userId, RatingTargetType.CONCERT, id);
        return ResponseEntity.ok().build();
    }
}
