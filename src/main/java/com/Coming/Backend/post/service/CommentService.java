package com.Coming.Backend.post.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.auth.service.UserStatusValidator;
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

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserStatusValidator userStatusValidator;

    /**
     * 게시글의 댓글·답글 목록을 조회한다. 최상위 댓글만 페이지네이션 대상이며, 답글은 각 최상위 댓글에 전체 포함된다.
     *
     * isPostAuthor는 현재 사용자와 무관하게 댓글 작성자와 게시글 작성자의 회원 id를 비교한다.
     *
     * @param userId 인증 사용자 ID. null이면 isLiked는 null, isAuthor는 false로 반환된다.
     */
    public PageResponse<CommentResponse> getComments(Long postId, Long userId, int page, int size) {
        Long postAuthorId = postRepository.findUserIdById(postId).orElseThrow(PostNotFoundException::new);

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> topLevelPage = commentRepository.findTopLevelByPostId(postId, pageable);
        List<Comment> topLevelComments = topLevelPage.getContent();

        List<Long> topLevelIds = topLevelComments.stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParentId = topLevelIds.isEmpty()
                ? Map.of()
                : commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(topLevelIds).stream()
                        .collect(Collectors.groupingBy(Comment::getParentCommentId));

        List<Comment> allComments = Stream.concat(
                topLevelComments.stream(),
                repliesByParentId.values().stream().flatMap(List::stream)
        ).toList();
        Map<Long, String> nicknameByUserId = findNicknames(allComments);

        // 소프트 삭제된 댓글은 좋아요 여부를 노출하지 않으므로 배치 조회 대상에서 제외한다.
        List<Comment> notDeletedComments = allComments.stream().filter(comment -> !comment.isDeleted()).toList();
        Set<Long> likedCommentIds = findLikedCommentIds(userId, notDeletedComments);
        CommentViewContext context = new CommentViewContext(userId, postAuthorId, nicknameByUserId, likedCommentIds);

        List<CommentResponse> content = topLevelComments.stream()
                .map(comment -> toResponse(comment, repliesByParentId.getOrDefault(comment.getId(), List.of()), context))
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
        userStatusValidator.validateWritable(userId);
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

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
        // commentCount는 삭제되지 않은 댓글·답글 수다. 소프트 삭제 시 softDelete에서 줄인다.
        postRepository.incrementCommentCount(postId);

        return new CommentCreateResponse(comment.getId());
    }

    /**
     * 댓글 또는 답글을 삭제한다. 작성자 본인만 삭제할 수 있다.
     * 하드 삭제 대신 소프트 삭제로 처리해 답글이 달린 댓글이 삭제돼도 답글은 그대로 유지된다.
     */
    @Transactional
    public void delete(Long userId, Long commentId) {
        userStatusValidator.validateWritable(userId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        if (!comment.isAuthoredBy(userId)) {
            throw new CommentForbiddenException();
        }
        softDelete(comment);
    }

    /**
     * 관리자가 작성자 검증 없이 댓글을 강제 삭제(소프트 삭제)한다. 신고 처리 등 관리자 조치용.
     */
    @Transactional
    public void adminDelete(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        softDelete(comment);
    }

    /**
     * 댓글을 소프트 삭제하고 게시글의 commentCount를 줄인다. 조건부 UPDATE로 실제 삭제한 요청만 줄여,
     * 같은 댓글을 동시에 삭제해도 두 번 줄지 않는다.
     */
    private void softDelete(Comment comment) {
        if (comment.isDeleted()) {
            return;
        }
        if (commentRepository.softDeleteIfNotDeleted(comment.getId()) == 1) {
            postRepository.decrementCommentCount(comment.getPostId());
        }
    }

    /**
     * 댓글에 좋아요를 남긴다. 이미 좋아요한 댓글이면 AlreadyLikedException을 던진다.
     */
    @Transactional
    public CommentLikeCountResponse like(Long userId, Long commentId) {
        userStatusValidator.validateWritable(userId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        if (comment.isDeleted()) {
            throw new CommentNotFoundException();
        }
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
        return new CommentLikeCountResponse(commentRepository.findLikeCountById(commentId));
    }

    /**
     * 댓글 좋아요를 취소한다. 좋아요한 적 없으면 NotLikedException을 던진다.
     */
    @Transactional
    public CommentLikeCountResponse unlike(Long userId, Long commentId) {
        userStatusValidator.validateWritable(userId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
        if (comment.isDeleted()) {
            throw new CommentNotFoundException();
        }
        CommentLike like = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(NotLikedException::new);
        commentLikeRepository.delete(like);
        commentRepository.decrementLikeCount(commentId);
        return new CommentLikeCountResponse(commentRepository.findLikeCountById(commentId));
    }

    private CommentResponse toResponse(Comment comment, List<Comment> replies, CommentViewContext context) {
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> toResponse(reply, List.of(), context))
                .toList();

        String authorNickname = context.nicknameByUserId().get(comment.getUserId());
        Boolean isLiked = context.viewerId() == null || comment.isDeleted()
                ? null
                : context.likedCommentIds().contains(comment.getId());

        return new CommentResponse(
                comment.getId(),
                authorNickname,
                comment.isVisibleAuthor(context.viewerId()),
                comment.isVisibleAuthor(context.postAuthorId()),
                comment.getDisplayContent(),
                comment.isDeleted(),
                comment.getLikeCount(),
                isLiked,
                comment.getCreatedAt(),
                replyResponses
        );
    }

    private Map<Long, String> findNicknames(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        // 탈퇴 회원은 닉네임이 null이라 toMap에 넣을 수 없으므로 제외한다(조회 시 null로 노출).
        return userRepository.findAllByIdIn(userIds).stream()
                .filter(user -> user.getNickname() != null)
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }

    private Set<Long> findLikedCommentIds(Long userId, List<Comment> comments) {
        if (userId == null || comments.isEmpty()) {
            return Set.of();
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        return new HashSet<>(commentLikeRepository.findLikedCommentIds(userId, commentIds));
    }

    /**
     * 댓글 응답 변환에 필요한 조회 결과. viewerId는 현재 사용자(비인증이면 null), postAuthorId는 게시글 작성자다.
     */
    private record CommentViewContext(Long viewerId, Long postAuthorId,
                                      Map<Long, String> nicknameByUserId, Set<Long> likedCommentIds) {
    }
}
