package com.Coming.Backend.user.controller;

import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.calendar.service.CalendarService;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryListItemResponse;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.service.InquiryService;
import com.Coming.Backend.user.dto.ConcertHistoryResponse;
import com.Coming.Backend.user.service.UserService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final CalendarService calendarService;
    private final UserService userService;
    private final InquiryService inquiryService;

    @Operation(summary = "예정·진행 중 공연 목록 조회")
    @GetMapping("/concerts/upcoming")
    public ResponseEntity<PageResponse<CalendarEntryResponse>> getUpcomingConcerts(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(calendarService.getMyCalendar(userId, pageable));
    }

    @Operation(summary = "다녀온 공연 목록 조회")
    @GetMapping("/concerts/history")
    public ResponseEntity<PageResponse<ConcertHistoryResponse>> getConcertHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getHistory(userId, pageable));
    }

    @Operation(summary = "내 문의 목록 조회")
    @GetMapping("/inquiries")
    public ResponseEntity<PageResponse<InquiryListItemResponse>> getInquiries(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(userId, status, pageable));
    }

    @Operation(summary = "내 문의 상세 조회")
    @ApiResponse(responseCode = "404", description = "INQUIRY_NOT_FOUND")
    @GetMapping("/inquiries/{id}")
    public ResponseEntity<InquiryDetailResponse> getInquiryDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(inquiryService.getMyInquiryDetail(userId, id));
    }
}
