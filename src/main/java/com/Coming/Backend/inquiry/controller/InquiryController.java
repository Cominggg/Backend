package com.Coming.Backend.inquiry.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryListItemResponse;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inquiry")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 등록")
    @ApiResponse(responseCode = "409", description = "INQUIRY_ALREADY_PENDING")
    @ApiResponse(responseCode = "404", description = "TARGET_NOT_FOUND")
    @PostMapping
    public ResponseEntity<Void> createInquiry(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InquiryCreateRequest request) {
        inquiryService.createInquiry(userId, request);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "내 문의 목록 조회")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<InquiryListItemResponse>> getMyInquiries(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(userId, status, pageable));
    }

    @Operation(summary = "내 문의 상세 조회")
    @ApiResponse(responseCode = "404", description = "INQUIRY_NOT_FOUND")
    @GetMapping("/my/{id}")
    public ResponseEntity<InquiryDetailResponse> getMyInquiryDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(inquiryService.getMyInquiryDetail(userId, id));
    }
}
