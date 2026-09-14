package com.Coming.Backend.post.controller;

import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.service.MentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Post")
@Validated
@RestController
@RequestMapping("/api/mentions")
@RequiredArgsConstructor
public class MentionController {

    private final MentionService mentionService;

    @Operation(summary = "엔티티 검색 (게시글 본문 멘션 자동완성)")
    @ApiResponse(responseCode = "400", description = "INVALID_INPUT (q 공백 또는 limit 범위 초과)")
    @GetMapping("/search")
    public ResponseEntity<List<EntityCardResponse>> search(
            @RequestParam EntityType type,
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        return ResponseEntity.ok(mentionService.search(type, q, limit));
    }
}
