package com.Coming.Backend.post.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.service.PostService;
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

@Tag(name = "Post")
@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;

    @Operation(summary = "게시글 통합 검색")
    @ApiResponse(responseCode = "400", description = "INVALID_INPUT (q 공백 또는 trim 후 2자 미만)")
    @GetMapping
    public ResponseEntity<PageResponse<PostSummaryResponse>> search(
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(postService.search(q, page, size));
    }
}
