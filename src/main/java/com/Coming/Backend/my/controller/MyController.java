package com.Coming.Backend.my.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.my.dto.ConcertHistoryResponse;
import com.Coming.Backend.my.service.MyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyController {

    private final MyService myService;

    @Operation(summary = "다녀온 공연 목록 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/history")
    public ResponseEntity<PageResponse<ConcertHistoryResponse>> getHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(myService.getHistory(userId, pageable));
    }
}
