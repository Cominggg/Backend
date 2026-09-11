package com.Coming.Backend.post.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.CommentCreateRequest;
import com.Coming.Backend.post.dto.CommentCreateResponse;
import com.Coming.Backend.post.dto.CommentLikeCountResponse;
import com.Coming.Backend.post.dto.CommentResponse;
import com.Coming.Backend.post.entity.Comment;
import com.Coming.Backend.post.entity.CommentLike;
import com.Coming.Backend.post.exception.AlreadyLikedException;
import com.Coming.Backend.post.exception.CommentForbiddenException;
import com.Coming.Backend.post.exception.CommentNotFoundException;
import com.Coming.Backend.post.exception.InvalidReplyDepthException;
import com.Coming.Backend.post.exception.NotLikedException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.repository.CommentLikeRepository;
import com.Coming.Backend.post.repository.CommentRepository;
import com.Coming.Backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final String DELETED_CONTENT_PLACEHOLDER = "삭제된 댓글입니다";

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 게시글의 댓글·답글 목록을 조회한다. 최상위 댓글만 페이지네이션 대상이며, 답글은 각 최상위 댓글에 전체 포함된다.
     *
     * @param userId 인증 사용자 ID. null이면 isLiked는 null, isAuthor는 false로 반환된다.
     */
    public PageResponse<CommentResponse> getComments(Long postId, Long userId, int page, int size) {
        postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> topLevelPage = commentRepository.findTopLevelByPostId(postId, pageable);
        List<Comment> topLevelComments = topLevelPage.getContent();

        List<Long> topLevelIds = topLevelComments.stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParentId = commentRepository.findByParentCommentIdInOrderByCreatedAtAsc(topLevelIds)
                .stream()
                .collect(Collectors.groupingBy(Comment::getParentCommentId));

        List<Comment> allComments = Stream.concat(
                topLevelComments.stream(),
                repliesByParentId.values().stream().flatMap(List::stream)
        ).toList();
        Map<Long, String> nicknameByUserId = findNicknames(allComments);
        Set<Long> likedCommentIds = findLikedCommentIds(userId, allComments);

        List<CommentResponse> content = topLevelComments.stream()
                .map(comment -> toResponse(comment, repliesByParentId.getOrDefault(comment.getId(), List.of()),
                        userId, nicknameByUserId, likedCommentIds))
                .toList();

        return new PageResponse<>(content, topLevelPage.getNumber(), topLevelPage.getSize(),
                topLevelPage.getTotalElements(), topLevelPage.getTotalPages());
    }

    /**
     * 댓글 또는 답글을 작성한다. parentCommentId가 주어지면 답글로 작성하며, 상위 댓글이 이미 답글이면
     * InvalidReplyDepthException을 던진다(2단계 이상 중첩 금지).
     */
    @Transactional
    public CommentCreateResponse create(Long userId, Long postId, CommentCreateRequest request) {
        postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        Long parentCommentId = request.parentCommentId();
        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                    .filter(comment -> comment.getPostId().equals(postId))
                    .orElseThrow(CommentNotFoundException::new);
            if (parent.isReply()) {
                throw new InvalidReplyDepthException();
            }
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentCommentId(parentCommentId)
                .content(request.content())
                .likeCount(0L)
                .deleted(false)
                .build();
        commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);

        return new CommentCreateResponse(comment.getId());
    }

    /**
     * 댓글 또는 답글을 삭제한다. 작성자 본인만 삭제할 수 있다.
     * 하드 삭제 대신 소프트 삭제로 처리해 답글이 달린 댓글이 삭제돼도 답글은 그대로 유지된다.
     */
    @Transactional
    public void delete(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        if (!comment.isAuthoredBy(userId)) {
            throw new CommentForbiddenException();
        }
        comment.softDelete();
    }

    /**
     * 댓글에 좋아요를 남긴다. 이미 좋아요한 댓글이면 AlreadyLikedException을 던진다.
     */
    @Transactional
    public CommentLikeCountResponse like(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new AlreadyLikedException();
        }
        try {
            commentLikeRepository.save(CommentLike.builder()
                    .userId(userId)
                    .commentId(commentId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyLikedException();
        }
        commentRepository.incrementLikeCount(commentId);
        return new CommentLikeCountResponse(comment.getLikeCount() + 1);
    }

    /**
     * 댓글 좋아요를 취소한다. 좋아요한 적 없으면 NotLikedException을 던진다.
     */
    @Transactional
    public CommentLikeCountResponse unlike(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        CommentLike like = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(NotLikedException::new);
        commentLikeRepository.delete(like);
        commentRepository.decrementLikeCount(commentId);
        return new CommentLikeCountResponse(comment.getLikeCount() - 1);
    }

    private CommentResponse toResponse(Comment comment, List<Comment> replies, Long userId,
            Map<Long, String> nicknameByUserId, Set<Long> likedCommentIds) {
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> toResponse(reply, List.of(), userId, nicknameByUserId, likedCommentIds))
                .toList();

        if (comment.isDeleted()) {
            return new CommentResponse(comment.getId(), null, false, DELETED_CONTENT_PLACEHOLDER, 0L, null,
                    comment.getCreatedAt(), replyResponses);
        }
        return new CommentResponse(
                comment.getId(),
                nicknameByUserId.get(comment.getUserId()),
                comment.isAuthoredBy(userId),
                comment.getContent(),
                comment.getLikeCount(),
                userId == null ? null : likedCommentIds.contains(comment.getId()),
                comment.getCreatedAt(),
                replyResponses
        );
    }

    private Map<Long, String> findNicknames(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        return userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }

    private Set<Long> findLikedCommentIds(Long userId, List<Comment> comments) {
        if (userId == null || comments.isEmpty()) {
            return Set.of();
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        return new HashSet<>(commentLikeRepository.findLikedCommentIds(userId, commentIds));
    }
}
