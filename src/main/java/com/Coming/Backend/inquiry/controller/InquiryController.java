package com.Coming.Backend.inquiry.controller;

import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
