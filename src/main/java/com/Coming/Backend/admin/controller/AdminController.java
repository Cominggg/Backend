package com.Coming.Backend.admin.controller;

import com.Coming.Backend.admin.dto.AdminArtistCreateRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "아티스트 등록")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/artists")
    public ResponseEntity<Void> createArtist(@RequestBody @Valid AdminArtistCreateRequest request) {
        adminService.createArtist(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "아티스트 정보 수정")
    @ApiResponse(responseCode = "404", description = "ARTIST_NOT_FOUND")
    @PutMapping("/artists/{id}")
    public ResponseEntity<Void> updateArtist(
            @PathVariable Long id,
            @RequestBody @Valid AdminArtistUpdateRequest request) {
        adminService.updateArtist(id, request);
        return ResponseEntity.ok().build();
    }
}
