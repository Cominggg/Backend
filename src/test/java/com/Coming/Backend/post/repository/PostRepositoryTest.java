package com.Coming.Backend.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.entity.Comment;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.entity.PostEntityTag;
import com.Coming.Backend.release.entity.Track;
import com.Coming.Backend.release.repository.TrackRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StreamUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostEntityTagRepository postEntityTagRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long AUTHOR_ID = 1L;
    private static final List<ConcertStatus> HIDDEN = List.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING);

    private String likeQuery(String q) {
        return "%" + q.toLowerCase(Locale.ROOT) + "%";
    }

    private Post buildPost(String title, String contentText) {
        return Post.builder()
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title(title)
                .content("{\"type\":\"doc\"}")
                .contentText(contentText)
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .build();
    }

    private Post buildPostWithRecommendCount(String title, long recommendCount) {
        return Post.builder()
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title(title)
                .content("{\"type\":\"doc\"}")
                .contentText("내용")
                .recommendCount(recommendCount)
                .viewCount(0L)
                .commentCount(0L)
                .build();
    }

    private Concert saveConcert(String title, ConcertStatus status) {
        return concertRepository.save(Concert.builder()
                .title(title)
                .startDate(LocalDate.of(2026, 12, 20))
                .endDate(LocalDate.of(2026, 12, 21))
                .venueName("KSPO DOME")
                .status(status)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.of(2026, 11, 1))
                .build());
    }

    private Post saveConcertTaggedPost(Concert concert) {
        // 게시글 제목·본문에는 검색어가 없어 공연 태그로만 매칭되도록 한다
        Post post = postRepository.save(buildPost("이 공연 가요", "같이 갈 사람"));
        postEntityTagRepository.save(PostEntityTag.builder()
                .postId(post.getId())
                .entityType(EntityType.CONCERT)
                .entityId(concert.getId())
                .build());
        return post;
    }

    @Test
    void should_return_post_when_title_matches_search_query() {
        // given
        Post post = postRepository.save(buildPost("아이유 단독 콘서트 후기", "정말 좋았다"));

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_return_post_when_content_text_matches_search_query() {
        // given
        Post post = postRepository.save(buildPost("공연 후기", "정말 재밌는 아이유 콘서트였다"));

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_return_post_when_tagged_artist_name_matches_search_query() {
        // given
        Artist artist = artistRepository.save(Artist.builder()
                .mbid(UUID.randomUUID().toString())
                .name("아이유")
                .isComing(false)
                .build());
        Post post = postRepository.save(buildPost("이 가수 좋아요", "최근에 알게 됐어요"));
        postEntityTagRepository.save(PostEntityTag.builder()
                .postId(post.getId())
                .entityType(EntityType.ARTIST)
                .entityId(artist.getId())
                .build());

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_return_post_when_tagged_track_title_matches_search_query() {
        // given
        Track track = trackRepository.save(Track.builder()
                .releaseGroupId(1L)
                .title("LILAC")
                .position(1)
                .build());
        Post post = postRepository.save(buildPost("이 곡 좋아요", "요즘 계속 듣는 중"));
        postEntityTagRepository.save(PostEntityTag.builder()
                .postId(post.getId())
                .entityType(EntityType.TRACK)
                .entityId(track.getId())
                .build());

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("LILAC"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_return_post_when_tagged_public_concert_title_matches_search_query() {
        // given
        Concert concert = saveConcert("아이유 HER 월드투어", ConcertStatus.UPCOMING);
        Post post = saveConcertTaggedPost(concert);

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("HER 월드투어"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_not_return_post_when_tagged_concert_is_excluded() {
        // given
        Concert concert = saveConcert("아이유 HER 월드투어 제외", ConcertStatus.EXCLUDED);
        Post post = saveConcertTaggedPost(concert);

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("HER 월드투어"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).doesNotContain(post.getId());
    }

    @Test
    void should_not_return_post_when_tagged_concert_is_pending() {
        // given
        Concert concert = saveConcert("아이유 HER 월드투어 대기", ConcertStatus.PENDING);
        Post post = saveConcertTaggedPost(concert);

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("HER 월드투어"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).doesNotContain(post.getId());
    }

    @Test
    void should_return_empty_page_when_no_post_matches_search_query() {
        // given
        postRepository.save(buildPost("다른 제목", "다른 내용"));

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("존재하지않는검색어"), HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_return_post_when_tagged_track_belongs_to_release() {
        // given
        Long releaseGroupId = 1L;
        Track track = trackRepository.save(Track.builder()
                .releaseGroupId(releaseGroupId)
                .title("LILAC")
                .position(1)
                .build());
        Post post = postRepository.save(buildPost("이 앨범의 이 곡이 좋아요", "명곡이다"));
        postEntityTagRepository.save(PostEntityTag.builder()
                .postId(post.getId())
                .entityType(EntityType.TRACK)
                .entityId(track.getId())
                .build());

        // when
        Page<Post> result = postRepository.findByEntityTag(EntityType.RELEASE, releaseGroupId, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_not_return_post_when_tagged_track_belongs_to_other_release() {
        // given
        Track track = trackRepository.save(Track.builder()
                .releaseGroupId(2L)
                .title("다른 앨범 트랙")
                .position(1)
                .build());
        Post post = postRepository.save(buildPost("다른 앨범 곡 얘기", "내용"));
        postEntityTagRepository.save(PostEntityTag.builder()
                .postId(post.getId())
                .entityType(EntityType.TRACK)
                .entityId(track.getId())
                .build());

        // when
        Page<Post> result = postRepository.findByEntityTag(EntityType.RELEASE, 1L, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).doesNotContain(post.getId());
    }

    @Test
    void should_return_current_recommend_count_when_finding_recommend_count_by_id() {
        // given
        Post post = Post.builder()
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title("제목")
                .content("{\"type\":\"doc\"}")
                .contentText("내용")
                .recommendCount(5L)
                .viewCount(0L)
                .commentCount(0L)
                .build();
        Post saved = postRepository.save(post);

        // when
        Long recommendCount = postRepository.findRecommendCountById(saved.getId());

        // then
        assertThat(recommendCount).isEqualTo(5L);
    }

    @Test
    void should_return_author_id_when_finding_user_id_by_existing_post_id() {
        // given
        Post saved = postRepository.save(buildPost("작성자 조회", "내용"));

        // when
        Optional<Long> userId = postRepository.findUserIdById(saved.getId());

        // then
        assertThat(userId).contains(AUTHOR_ID);
    }

    @Test
    void should_return_empty_when_finding_user_id_by_non_existent_post_id() {
        // given
        Long nonExistentPostId = -1L;

        // when
        Optional<Long> userId = postRepository.findUserIdById(nonExistentPostId);

        // then
        assertThat(userId).isEmpty();
    }

    @Test
    void should_return_posts_ordered_by_created_at_desc_when_multiple_posts_match() throws InterruptedException {
        // given
        Post olderPost = postRepository.save(buildPost("아이유 1번째 글", "내용"));
        Thread.sleep(10);
        Post newerPost = postRepository.save(buildPost("아이유 2번째 글", "내용"));

        // when
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), HIDDEN, pageable);

        // then
        assertThat(result.getContent()).extracting(Post::getId)
                .containsSubsequence(newerPost.getId(), olderPost.getId());
    }

    // -------------------------------------------------------------------------
    // findPopularBoard
    // -------------------------------------------------------------------------

    @Test
    void should_return_post_when_recommend_count_is_at_or_above_threshold() {
        // given
        Post qualifyingPost = postRepository.save(buildPostWithRecommendCount("추천수 딱 임계치", 10L));

        // when
        Page<Post> result = postRepository.findPopularBoard(10L, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(qualifyingPost.getId());
    }

    @Test
    void should_exclude_post_when_recommend_count_is_below_threshold() {
        // given
        Post belowThresholdPost = postRepository.save(buildPostWithRecommendCount("추천수 미달", 9L));

        // when
        Page<Post> result = postRepository.findPopularBoard(10L, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).doesNotContain(belowThresholdPost.getId());
    }

    @Test
    void should_return_posts_ordered_by_created_at_desc_when_finding_popular_board() throws InterruptedException {
        // given
        Post olderPost = postRepository.save(buildPostWithRecommendCount("먼저 쓴 인기글", 10L));
        Thread.sleep(10);
        Post newerPost = postRepository.save(buildPostWithRecommendCount("나중에 쓴 인기글", 20L));

        // when
        Page<Post> result = postRepository.findPopularBoard(10L, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId)
                .containsSubsequence(newerPost.getId(), olderPost.getId());
    }

    @Test
    void should_persist_and_clear_cover_concert_id_when_cover_concert_changed() {
        // given
        Post post = postRepository.save(buildPost("대표 공연 저장", "내용"));

        // when
        post.changeCoverConcert(42L);
        entityManager.flush();
        entityManager.clear();
        Post saved = postRepository.findById(post.getId()).orElseThrow();
        Long savedCoverConcertId = saved.getCoverConcertId();
        saved.changeCoverConcert(null);
        entityManager.flush();
        entityManager.clear();
        Post cleared = postRepository.findById(post.getId()).orElseThrow();

        // then
        assertThat(savedCoverConcertId).isEqualTo(42L);
        assertThat(cleared.getCoverConcertId()).isNull();
    }

    // -------------------------------------------------------------------------
    // decrementCommentCount
    // -------------------------------------------------------------------------

    private Post savePostWithCommentCount(String title, long commentCount) {
        return postRepository.save(Post.builder()
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title(title)
                .content("{\"type\":\"doc\"}")
                .contentText("내용")
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(commentCount)
                .build());
    }

    private Long findCommentCount(Long postId) {
        entityManager.flush();
        entityManager.clear();
        return postRepository.findById(postId).orElseThrow().getCommentCount();
    }

    @Test
    void should_decrease_comment_count_by_one_when_decrementing_comment_count() {
        // given
        Post post = savePostWithCommentCount("댓글 3개 게시글", 3L);
        entityManager.flush();

        // when
        postRepository.decrementCommentCount(post.getId());

        // then
        assertThat(findCommentCount(post.getId())).isEqualTo(2L);
    }

    @Test
    void should_keep_comment_count_zero_when_decrementing_comment_count_of_zero() {
        // given
        Post post = savePostWithCommentCount("댓글 없는 게시글", 0L);
        entityManager.flush();

        // when
        postRepository.decrementCommentCount(post.getId());

        // then
        assertThat(findCommentCount(post.getId())).isZero();
    }

    // -------------------------------------------------------------------------
    // V45__recount_post_comment_count.sql
    // -------------------------------------------------------------------------

    private Comment saveComment(Long postId, Long parentCommentId, boolean deleted) {
        return commentRepository.save(Comment.builder()
                .postId(postId)
                .userId(AUTHOR_ID)
                .parentCommentId(parentCommentId)
                .content("댓글")
                .likeCount(0L)
                .deleted(deleted)
                .build());
    }

    @Test
    void should_recount_comment_count_as_not_deleted_comments_when_v45_migration_runs() throws IOException {
        // given
        Post postWithComments = savePostWithCommentCount("댓글 있는 게시글", 10L);
        Post postWithoutComments = savePostWithCommentCount("댓글 없는 게시글", 4L);
        Comment liveTop = saveComment(postWithComments.getId(), null, false);
        Comment deletedTop = saveComment(postWithComments.getId(), null, true);
        saveComment(postWithComments.getId(), liveTop.getId(), false);
        saveComment(postWithComments.getId(), liveTop.getId(), true);
        saveComment(postWithComments.getId(), deletedTop.getId(), false);
        entityManager.flush();
        String migrationSql = StreamUtils.copyToString(
                new ClassPathResource("db/migration/V45__recount_post_comment_count.sql").getInputStream(),
                StandardCharsets.UTF_8);

        // when
        entityManager.getEntityManager().createNativeQuery(migrationSql).executeUpdate();

        // then
        assertThat(findCommentCount(postWithComments.getId())).isEqualTo(3L);
        assertThat(findCommentCount(postWithoutComments.getId())).isZero();
    }
}
