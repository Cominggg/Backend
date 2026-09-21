package com.Coming.Backend.notice.controller;

import com.Coming.Backend.notice.dto.NoticeDetailResponse;
import com.Coming.Backend.notice.dto.NoticeSummaryResponse;
import com.Coming.Backend.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notice")
@Validated
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "커뮤니티 홈 상단 고정용 최근 공지사항 조회")
    @GetMapping
    public ResponseEntity<List<NoticeSummaryResponse>> getRecent(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return ResponseEntity.ok(noticeService.getRecent(limit));
    }

    @Operation(summary = "공지사항 상세 조회")
    @ApiResponse(responseCode = "404", description = "NOTICE_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<NoticeDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.getDetail(id));
    }
}
