package com.Coming.Backend.admin.controller;

import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertArtistAssignRequest;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.AdminPendingConcertResponse;
import com.Coming.Backend.admin.service.AdminService;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "아티스트 정보 수정")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND")
    @PutMapping("/artists/{id}")
    public ResponseEntity<Void> updateArtist(
            @PathVariable Long id,
            @RequestBody @Valid AdminArtistUpdateRequest request) {
        adminService.updateArtist(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "전체 문의 목록 조회")
    @GetMapping("/inquiries")
    public ResponseEntity<PageResponse<AdminInquiryListItemResponse>> getInquiries(
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getInquiries(type, status, pageable));
    }

    @Operation(summary = "문의 상세 조회")
    @ApiResponse(responseCode = "404", description = "INQUIRY_NOT_FOUND")
    @GetMapping("/inquiries/{id}")
    public ResponseEntity<AdminInquiryDetailResponse> getInquiryDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getInquiryDetail(id));
    }

    @Operation(summary = "문의 처리 상태 변경")
    @ApiResponse(responseCode = "404", description = "INQUIRY_NOT_FOUND")
    @PatchMapping("/inquiries/{id}/status")
    public ResponseEntity<Void> updateInquiryStatus(
            @PathVariable Long id,
            @RequestBody @Valid AdminInquiryStatusUpdateRequest request) {
        adminService.updateInquiryStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "PENDING 공연 목록 조회")
    @GetMapping("/concerts/pending")
    public ResponseEntity<PageResponse<AdminPendingConcertResponse>> getPendingConcerts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingConcerts(pageable));
    }

    @Operation(summary = "공연 정보 수정")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @PutMapping("/concerts/{id}")
    public ResponseEntity<Void> updateConcert(
            @PathVariable Long id,
            @RequestBody @Valid AdminConcertUpdateRequest request) {
        adminService.updateConcert(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "공연 상태 강제 변경")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @PutMapping("/concerts/{id}/state")
    public ResponseEntity<Void> forceChangeConcertState(
            @PathVariable Long id,
            @RequestBody @Valid AdminConcertStateUpdateRequest request) {
        adminService.forceChangeConcertState(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "PENDING 공연 승인")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @ApiResponse(responseCode = "400", description = "CONCERT_NOT_PENDING")
    @PutMapping("/concerts/{id}/approve")
    public ResponseEntity<Void> approveConcert(@PathVariable Long id) {
        adminService.approveConcert(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "PENDING 공연 거절")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND")
    @ApiResponse(responseCode = "400", description = "CONCERT_NOT_PENDING")
    @PutMapping("/concerts/{id}/reject")
    public ResponseEntity<Void> rejectConcert(@PathVariable Long id) {
        adminService.rejectConcert(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "공연 아티스트 직접 지정")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND / ARTIST_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CONCERT_ARTIST_ALREADY_EXISTS")
    @PostMapping("/concerts/{id}/artists")
    public ResponseEntity<Void> assignArtistToConcert(
            @PathVariable Long id,
            @RequestBody @Valid AdminConcertArtistAssignRequest request) {
        adminService.assignArtistToConcert(id, request.artistId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Data 파이프라인 아티스트 릴리즈 수집 트리거")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND (Data 파이프라인 측)")
    @PostMapping("/data/collect/artists/{id}/releases")
    public ResponseEntity<Void> triggerArtistReleases(@PathVariable Long id) {
        adminService.triggerArtistReleases(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Data 파이프라인 셋리스트 수집 트리거")
    @ApiResponse(responseCode = "404", description = "CONCERT_NOT_FOUND (Data 파이프라인 측)")
    @PostMapping("/data/collect/concerts/{id}/setlist")
    public ResponseEntity<Void> triggerConcertSetlist(@PathVariable Long id) {
        adminService.triggerConcertSetlist(id);
        return ResponseEntity.ok().build();
    }
}
