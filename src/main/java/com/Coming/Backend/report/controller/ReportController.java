package com.Coming.Backend.report.controller;

import com.Coming.Backend.report.dto.ReportCreateRequest;
import com.Coming.Backend.report.dto.ReportCreateResponse;
import com.Coming.Backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "게시글·댓글 신고")
    @ApiResponse(responseCode = "400", description = "REPORT_DETAIL_REQUIRED")
    @ApiResponse(responseCode = "404", description = "REPORT_TARGET_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "REPORT_ALREADY_EXISTS")
    @PostMapping
    public ResponseEntity<ReportCreateResponse> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ReportCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.create(userId, request));
    }
}
