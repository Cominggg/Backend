package com.Coming.Backend.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.post.entity.Comment;
import com.Coming.Backend.post.entity.CommentLike;
import jakarta.persistence.EntityManager;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private EntityManager entityManager;

    private static final Long AUTHOR_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long OTHER_POST_ID = 200L;

    private Comment buildComment(Long postId, Long parentCommentId, String content) {
        return buildComment(postId, parentCommentId, content, 0L);
    }

    private Comment buildComment(Long postId, Long parentCommentId, String content, long likeCount) {
        return Comment.builder()
                .postId(postId)
                .userId(AUTHOR_ID)
                .parentCommentId(parentCommentId)
                .content(content)
                .likeCount(likeCount)
                .deleted(false)
                .build();
    }

    @Test
    void should_return_only_top_level_comments_ordered_by_created_at_when_finding_top_level_by_post_id() {
        // given
        Comment first = commentRepository.save(buildComment(POST_ID, null, "첫 댓글"));
        Comment second = commentRepository.save(buildComment(POST_ID, null, "두번째 댓글"));
        commentRepository.save(buildComment(POST_ID, first.getId(), "답글은 제외"));
        commentRepository.save(buildComment(OTHER_POST_ID, null, "다른 게시글 댓글"));

        // when
        Page<Comment> result = commentRepository.findTopLevelByPostId(POST_ID, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Comment::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void should_return_empty_page_when_post_has_no_comments() {
        // when
        Page<Comment> result = commentRepository.findTopLevelByPostId(POST_ID, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void should_return_replies_ordered_by_created_at_when_parent_ids_given() {
        // given
        Comment parent = commentRepository.save(buildComment(POST_ID, null, "부모 댓글"));
        Comment firstReply = commentRepository.save(buildComment(POST_ID, parent.getId(), "첫 답글"));
        Comment secondReply = commentRepository.save(buildComment(POST_ID, parent.getId(), "두번째 답글"));

        // when
        List<Comment> replies = commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(List.of(parent.getId()));

        // then
        assertThat(replies).extracting(Comment::getId)
                .containsExactly(firstReply.getId(), secondReply.getId());
    }

    @Test
    void should_return_empty_list_without_error_when_parent_ids_is_empty() {
        // given
        commentRepository.save(buildComment(POST_ID, null, "댓글"));

        // when
        List<Comment> replies = commentRepository.findByParentCommentIdInOrderByCreatedAtAscIdAsc(List.of());

        // then
        assertThat(replies).isEmpty();
    }

    @Test
    void should_increment_like_count_when_incrementing() {
        // given
        Comment comment = commentRepository.save(buildComment(POST_ID, null, "댓글"));

        // when
        commentRepository.incrementLikeCount(comment.getId());
        entityManager.clear();
        Comment updated = commentRepository.findById(comment.getId()).orElseThrow();

        // then
        assertThat(updated.getLikeCount()).isEqualTo(1L);
    }

    @Test
    void should_decrement_like_count_when_decrementing() {
        // given
        Comment comment = commentRepository.save(buildComment(POST_ID, null, "댓글", 1L));

        // when
        commentRepository.decrementLikeCount(comment.getId());
        entityManager.clear();
        Comment updated = commentRepository.findById(comment.getId()).orElseThrow();

        // then
        assertThat(updated.getLikeCount()).isZero();
    }

    @Test
    void should_return_only_liked_comment_ids_when_finding_liked_comment_ids() {
        // given
        Comment liked = commentRepository.save(buildComment(POST_ID, null, "좋아요한 댓글"));
        Comment notLiked = commentRepository.save(buildComment(POST_ID, null, "좋아요 안한 댓글"));
        commentLikeRepository.save(CommentLike.builder().userId(AUTHOR_ID).commentId(liked.getId()).build());

        // when
        List<Long> likedIds = commentLikeRepository.findLikedCommentIds(AUTHOR_ID, List.of(liked.getId(), notLiked.getId()));

        // then
        assertThat(likedIds).containsExactly(liked.getId());
    }

    @Test
    void should_return_empty_list_without_error_when_comment_ids_is_empty_on_finding_liked_comment_ids() {
        // when
        List<Long> likedIds = commentLikeRepository.findLikedCommentIds(AUTHOR_ID, List.of());

        // then
        assertThat(likedIds).isEmpty();
    }

    @Test
    void should_return_only_comment_ids_belonging_to_post_when_finding_ids_by_post_id() {
        // given
        Comment first = commentRepository.save(buildComment(POST_ID, null, "댓글1"));
        Comment second = commentRepository.save(buildComment(POST_ID, first.getId(), "댓글2"));
        commentRepository.save(buildComment(OTHER_POST_ID, null, "다른 게시글 댓글"));

        // when
        List<Long> ids = commentRepository.findIdsByPostId(POST_ID);

        // then
        assertThat(ids).containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void should_delete_only_comments_belonging_to_post_when_deleting_by_post_id() {
        // given
        Comment target = commentRepository.save(buildComment(POST_ID, null, "삭제될 댓글"));
        Comment other = commentRepository.save(buildComment(OTHER_POST_ID, null, "유지될 댓글"));

        // when
        commentRepository.deleteByPostId(POST_ID);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(commentRepository.findById(target.getId())).isEmpty();
        assertThat(commentRepository.findById(other.getId())).isPresent();
    }

    @Test
    void should_delete_only_likes_for_given_comment_ids_when_deleting_by_comment_id_in() {
        // given
        Comment target = commentRepository.save(buildComment(POST_ID, null, "댓글"));
        Comment other = commentRepository.save(buildComment(POST_ID, null, "다른 댓글"));
        CommentLike targetLike = commentLikeRepository.save(
                CommentLike.builder().userId(AUTHOR_ID).commentId(target.getId()).build());
        CommentLike otherLike = commentLikeRepository.save(
                CommentLike.builder().userId(AUTHOR_ID).commentId(other.getId()).build());

        // when
        commentLikeRepository.deleteByCommentIdIn(List.of(target.getId()));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(commentLikeRepository.findById(targetLike.getId())).isEmpty();
        assertThat(commentLikeRepository.findById(otherLike.getId())).isPresent();
    }

    @Test
    void should_return_current_like_count_when_finding_like_count_by_id() {
        // given
        Comment comment = commentRepository.save(buildComment(POST_ID, null, "댓글", 3L));

        // when
        Long likeCount = commentRepository.findLikeCountById(comment.getId());

        // then
        assertThat(likeCount).isEqualTo(3L);
    }
}
