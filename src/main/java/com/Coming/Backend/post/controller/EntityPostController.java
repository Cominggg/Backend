package com.Coming.Backend.post.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.service.PostService;
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

@Tag(name = "Post")
@Validated
@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
public class EntityPostController {

    private final PostService postService;

    @Operation(summary = "엔티티별 게시글 백링크 조회")
    @ApiResponse(responseCode = "400", description = "INVALID_INPUT (허용되지 않은 sort 값)")
    @GetMapping("/{type}/{id}/posts")
    public ResponseEntity<PageResponse<PostSummaryResponse>> getBacklinks(
            @PathVariable EntityType type,
            @PathVariable Long id,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(postService.getBacklinks(type, id, sort, page, size));
    }
}
