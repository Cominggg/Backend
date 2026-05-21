package com.Coming.Backend.calendar.controller;

import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.calendar.service.CalendarService;
import com.Coming.Backend.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Calendar")
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "월별 공연 캘린더 조회")
    @GetMapping
    public ResponseEntity<List<CalendarEntryResponse>> getCalendar(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(calendarService.getCalendar(year, month, userId));
    }

    @Operation(summary = "내 캘린더 목록 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<CalendarEntryResponse>> getMyCalendar(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(calendarService.getMyCalendar(userId, pageable));
    }

    @Operation(summary = "내 캘린더에 공연 추가")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "ALREADY_IN_CALENDAR")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{concertId}")
    public ResponseEntity<Void> addToCalendar(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long concertId) {
        calendarService.addToCalendar(userId, concertId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 캘린더에서 공연 제거")
    @ApiResponse(responseCode = "400", description = "NOT_IN_CALENDAR")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{concertId}")
    public ResponseEntity<Void> removeFromCalendar(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long concertId) {
        calendarService.removeFromCalendar(userId, concertId);
        return ResponseEntity.ok().build();
    }
}
