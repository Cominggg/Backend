package com.Coming.Backend.post.controller;

import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.PostCreateRequest;
import com.Coming.Backend.post.dto.PostCreateResponse;
import com.Coming.Backend.post.dto.PostDetailResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.dto.PostUpdateRequest;
import com.Coming.Backend.post.dto.RecommendCountResponse;
import com.Coming.Backend.post.dto.TrendingTagResponse;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Post")
@Validated
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 작성")
    @PostMapping
    public ResponseEntity<PostCreateResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(userId, request));
    }

    @Operation(summary = "게시글 상세 조회")
    @ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getDetail(id, userId));
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<PostSummaryResponse>> getList(
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(postService.getList(category, page, size));
    }

    @Operation(summary = "이번 주 인기 게시글 조회")
    @GetMapping("/popular")
    public ResponseEntity<List<PostSummaryResponse>> getPopular(
            @RequestParam(defaultValue = "7") @Min(1) int days,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(postService.getPopular(days, limit));
    }

    @Operation(summary = "최근 많이 언급된 태그 조회")
    @GetMapping("/trending-tags")
    public ResponseEntity<List<TrendingTagResponse>> getTrendingTags(
            @RequestParam(defaultValue = "7") @Min(1) int days,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(postService.getTrendingTags(days, limit));
    }

    @Operation(summary = "게시글 수정")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN (작성자 본인 아님)")
    @ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request) {
        postService.update(userId, id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "게시글 삭제")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN (작성자 본인 아님)")
    @ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        postService.delete(userId, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "게시글 추천")
    @ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "ALREADY_RECOMMENDED")
    @PostMapping("/{id}/recommend")
    public ResponseEntity<RecommendCountResponse> recommend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(postService.recommend(userId, id));
    }

    @Operation(summary = "게시글 추천 취소")
    @ApiResponse(responseCode = "400", description = "NOT_RECOMMENDED")
    @ApiResponse(responseCode = "404", description = "POST_NOT_FOUND")
    @DeleteMapping("/{id}/recommend")
    public ResponseEntity<RecommendCountResponse> unrecommend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(postService.unrecommend(userId, id));
    }
}
