package com.Coming.Backend.post.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.exception.ErrorCode;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    private static final Long POST_ID = 100L;
    private static final Long OTHER_POST_ID = 200L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long VIEWER_ID = 3L;
    private static final Long COMMENT_ID = 10L;
    private static final Long REPLY_ID = 11L;
    private static final Long PARENT_ID = 20L;

    private Comment buildComment(Long id, Long postId, Long userId, Long parentCommentId, String content,
            long likeCount, boolean deleted) {
        return Comment.builder()
                .id(id)
                .postId(postId)
                .userId(userId)
                .parentCommentId(parentCommentId)
                .content(content)
                .likeCount(likeCount)
                .deleted(deleted)
                .build();
    }

    private User buildUser(Long id, String nickname) {
        return User.builder().id(id).nickname(nickname).build();
    }

    // -------------------------------------------------------------------------
    // getComments
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_post_does_not_exist_on_get_comments() {
        // given
        given(postRepository.existsById(POST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.getComments(POST_ID, null, 0, 20))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_return_top_level_comments_with_nested_replies_when_comments_exist() {
        // given
        Comment topComment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "탑레벨 댓글", 2L, false);
        Comment reply = buildComment(REPLY_ID, POST_ID, OTHER_USER_ID, COMMENT_ID, "답글", 0L, false);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> topLevelPage = new PageImpl<>(List.of(topComment), pageable, 1);

        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findTopLevelByPostId(POST_ID, pageable)).willReturn(topLevelPage);
        given(commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(List.of(COMMENT_ID)))
                .willReturn(List.of(reply));
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID, OTHER_USER_ID)))
                .willReturn(List.of(buildUser(AUTHOR_ID, "IU"), buildUser(OTHER_USER_ID, "뷔")));

        // when
        PageResponse<CommentResponse> response = commentService.getComments(POST_ID, null, 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        CommentResponse topResponse = response.content().get(0);
        assertThat(topResponse.id()).isEqualTo(COMMENT_ID);
        assertThat(topResponse.replies()).hasSize(1);
        assertThat(topResponse.replies().get(0).id()).isEqualTo(REPLY_ID);
        assertThat(topResponse.replies().get(0).replies()).isEmpty();
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void should_return_null_is_liked_and_false_is_author_when_user_id_not_given() {
        // given
        Comment topComment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 1L, false);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> topLevelPage = new PageImpl<>(List.of(topComment), pageable, 1);

        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findTopLevelByPostId(POST_ID, pageable)).willReturn(topLevelPage);
        given(commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(List.of(COMMENT_ID)))
                .willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<CommentResponse> response = commentService.getComments(POST_ID, null, 0, 20);

        // then
        CommentResponse topResponse = response.content().get(0);
        assertThat(topResponse.isLiked()).isNull();
        assertThat(topResponse.isAuthor()).isFalse();
    }

    @Test
    void should_mark_comment_as_liked_when_user_has_liked_it() {
        // given
        Comment topComment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 1L, false);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> topLevelPage = new PageImpl<>(List.of(topComment), pageable, 1);

        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findTopLevelByPostId(POST_ID, pageable)).willReturn(topLevelPage);
        given(commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(List.of(COMMENT_ID)))
                .willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));
        given(commentLikeRepository.findLikedCommentIds(VIEWER_ID, List.of(COMMENT_ID)))
                .willReturn(List.of(COMMENT_ID));

        // when
        PageResponse<CommentResponse> response = commentService.getComments(POST_ID, VIEWER_ID, 0, 20);

        // then
        assertThat(response.content().get(0).isLiked()).isTrue();
    }

    @Test
    void should_replace_content_but_show_nickname_and_like_count_when_comment_is_deleted() {
        // given
        Comment deletedTopComment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "삭제될 댓글", 3L, true);
        Comment reply = buildComment(REPLY_ID, POST_ID, OTHER_USER_ID, COMMENT_ID, "답글", 0L, false);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Comment> topLevelPage = new PageImpl<>(List.of(deletedTopComment), pageable, 1);

        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findTopLevelByPostId(POST_ID, pageable)).willReturn(topLevelPage);
        given(commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(List.of(COMMENT_ID)))
                .willReturn(List.of(reply));
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID, OTHER_USER_ID)))
                .willReturn(List.of(buildUser(AUTHOR_ID, "지민"), buildUser(OTHER_USER_ID, "뷔")));
        given(commentLikeRepository.findLikedCommentIds(VIEWER_ID, List.of(REPLY_ID)))
                .willReturn(List.of());

        // when
        PageResponse<CommentResponse> response = commentService.getComments(POST_ID, VIEWER_ID, 0, 20);

        // then
        CommentResponse topResponse = response.content().get(0);
        assertThat(topResponse.content()).isEqualTo("삭제된 댓글입니다");
        assertThat(topResponse.authorNickname()).isEqualTo("지민");
        assertThat(topResponse.likeCount()).isEqualTo(3L);
        assertThat(topResponse.isLiked()).isNull();
        assertThat(topResponse.isAuthor()).isFalse();
        assertThat(topResponse.replies()).hasSize(1);
        assertThat(topResponse.replies().get(0).content()).isEqualTo("답글");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_post_does_not_exist_on_create() {
        // given
        given(postRepository.existsById(POST_ID)).willReturn(false);
        CommentCreateRequest request = new CommentCreateRequest("내용", null);

        // when & then
        assertThatThrownBy(() -> commentService.create(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_save_top_level_comment_and_increment_comment_count_when_parent_comment_id_is_null() {
        // given
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", COMMENT_ID);
            return saved;
        });
        CommentCreateRequest request = new CommentCreateRequest("내용", null);

        // when
        CommentCreateResponse response = commentService.create(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(response.id()).isEqualTo(COMMENT_ID);
        verify(postRepository).incrementCommentCount(POST_ID);
    }

    @Test
    void should_throw_comment_not_found_exception_when_parent_comment_does_not_exist() {
        // given
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findById(PARENT_ID)).willReturn(Optional.empty());
        CommentCreateRequest request = new CommentCreateRequest("답글 내용", PARENT_ID);

        // when & then
        assertThatThrownBy(() -> commentService.create(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
        verify(postRepository, never()).incrementCommentCount(any());
    }

    @Test
    void should_throw_comment_not_found_exception_when_parent_comment_belongs_to_other_post() {
        // given
        Comment parent = buildComment(PARENT_ID, OTHER_POST_ID, AUTHOR_ID, null, "다른 게시글 댓글", 0L, false);
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));
        CommentCreateRequest request = new CommentCreateRequest("답글 내용", PARENT_ID);

        // when & then
        assertThatThrownBy(() -> commentService.create(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_invalid_reply_depth_exception_when_parent_comment_is_already_a_reply() {
        // given
        Comment parent = buildComment(PARENT_ID, POST_ID, AUTHOR_ID, COMMENT_ID, "이미 답글인 댓글", 0L, false);
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(commentRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));
        CommentCreateRequest request = new CommentCreateRequest("답글 내용", PARENT_ID);

        // when & then
        assertThatThrownBy(() -> commentService.create(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(InvalidReplyDepthException.class)
                .hasMessage(ErrorCode.INVALID_REPLY_DEPTH.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void should_throw_comment_not_found_exception_when_comment_does_not_exist_on_delete() {
        // given
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.delete(AUTHOR_ID, COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_comment_forbidden_exception_when_deleter_is_not_author() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 0L, false);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.delete(OTHER_USER_ID, COMMENT_ID))
                .isInstanceOf(CommentForbiddenException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void should_soft_delete_comment_when_author_deletes() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 0L, false);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        // when
        commentService.delete(AUTHOR_ID, COMMENT_ID);

        // then
        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    // -------------------------------------------------------------------------
    // like
    // -------------------------------------------------------------------------

    @Test
    void should_throw_comment_not_found_exception_when_comment_does_not_exist_on_like() {
        // given
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.like(AUTHOR_ID, COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_already_liked_exception_when_user_already_liked_comment() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 5L, false);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
        given(commentLikeRepository.existsByUserIdAndCommentId(OTHER_USER_ID, COMMENT_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> commentService.like(OTHER_USER_ID, COMMENT_ID))
                .isInstanceOf(AlreadyLikedException.class)
                .hasMessage(ErrorCode.ALREADY_LIKED.getMessage());
        verify(commentLikeRepository, never()).save(any(CommentLike.class));
        verify(commentRepository, never()).incrementLikeCount(any());
    }

    @Test
    void should_throw_comment_not_found_exception_when_liking_deleted_comment() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "삭제된 댓글", 5L, true);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.like(OTHER_USER_ID, COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
        verify(commentLikeRepository, never()).save(any(CommentLike.class));
        verify(commentRepository, never()).incrementLikeCount(any());
    }

    @Test
    void should_increment_like_count_and_return_incremented_count_when_user_has_not_liked_comment() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 5L, false);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
        given(commentLikeRepository.existsByUserIdAndCommentId(OTHER_USER_ID, COMMENT_ID)).willReturn(false);
        given(commentRepository.findLikeCountById(COMMENT_ID)).willReturn(6L);

        // when
        CommentLikeCountResponse response = commentService.like(OTHER_USER_ID, COMMENT_ID);

        // then
        assertThat(response.likeCount()).isEqualTo(6L);
        verify(commentRepository).incrementLikeCount(COMMENT_ID);
        verify(commentLikeRepository).save(any(CommentLike.class));
    }

    // -------------------------------------------------------------------------
    // unlike
    // -------------------------------------------------------------------------

    @Test
    void should_throw_comment_not_found_exception_when_comment_does_not_exist_on_unlike() {
        // given
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.unlike(AUTHOR_ID, COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_not_liked_exception_when_user_has_not_liked_comment() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 5L, false);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
        given(commentLikeRepository.findByUserIdAndCommentId(OTHER_USER_ID, COMMENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.unlike(OTHER_USER_ID, COMMENT_ID))
                .isInstanceOf(NotLikedException.class)
                .hasMessage(ErrorCode.NOT_LIKED.getMessage());
        verify(commentLikeRepository, never()).delete(any(CommentLike.class));
        verify(commentRepository, never()).decrementLikeCount(any());
    }

    @Test
    void should_throw_comment_not_found_exception_when_unliking_deleted_comment() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "삭제된 댓글", 5L, true);
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.unlike(OTHER_USER_ID, COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
        verify(commentLikeRepository, never()).delete(any(CommentLike.class));
        verify(commentRepository, never()).decrementLikeCount(any());
    }

    @Test
    void should_decrement_like_count_and_return_decremented_count_when_user_has_liked_comment() {
        // given
        Comment comment = buildComment(COMMENT_ID, POST_ID, AUTHOR_ID, null, "댓글", 5L, false);
        CommentLike like = CommentLike.builder().userId(OTHER_USER_ID).commentId(COMMENT_ID).build();
        given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
        given(commentLikeRepository.findByUserIdAndCommentId(OTHER_USER_ID, COMMENT_ID)).willReturn(Optional.of(like));
        given(commentRepository.findLikeCountById(COMMENT_ID)).willReturn(4L);

        // when
        CommentLikeCountResponse response = commentService.unlike(OTHER_USER_ID, COMMENT_ID);

        // then
        assertThat(response.likeCount()).isEqualTo(4L);
        verify(commentLikeRepository).delete(like);
        verify(commentRepository).decrementLikeCount(COMMENT_ID);
    }
}
