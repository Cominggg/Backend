package com.Coming.Backend.post.controller;

import com.Coming.Backend.post.dto.CommentLikeCountResponse;
import com.Coming.Backend.post.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comment")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 삭제")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN (작성자 본인 아님)")
    @ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId) {
        commentService.delete(userId, commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "댓글 좋아요")
    @ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "ALREADY_LIKED")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeCountResponse> like(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.like(userId, commentId));
    }

    @Operation(summary = "댓글 좋아요 취소")
    @ApiResponse(responseCode = "400", description = "NOT_LIKED")
    @ApiResponse(responseCode = "404", description = "COMMENT_NOT_FOUND")
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeCountResponse> unlike(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.unlike(userId, commentId));
    }
}
