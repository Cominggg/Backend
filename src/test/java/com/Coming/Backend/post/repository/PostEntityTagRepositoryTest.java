package com.Coming.Backend.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.entity.PostEntityTag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostEntityTagRepositoryTest {

    @Autowired
    private PostEntityTagRepository postEntityTagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ArtistRepository artistRepository;

    private static final Long AUTHOR_ID = 1L;
    private static final List<ConcertStatus> HIDDEN = List.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING);

    // 테스트 시작 이후 작성된 게시글만 집계되도록 해 DB에 남아 있는 다른 데이터의 영향을 막는다
    // (DB timestamp 정밀도가 마이크로초라 저장값이 since보다 작아지지 않도록 맞춰 둔다)
    private LocalDateTime since;

    @BeforeEach
    void setUp() {
        since = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
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

    private Artist saveArtist(String name) {
        return artistRepository.save(Artist.builder()
                .mbid(UUID.randomUUID().toString())
                .name(name)
                .isComing(false)
                .build());
    }

    private void saveTaggedPosts(EntityType entityType, Long entityId, int postCount) {
        for (int i = 0; i < postCount; i++) {
            Post post = postRepository.save(Post.builder()
                    .userId(AUTHOR_ID)
                    .category(PostCategory.FREE)
                    .title("언급 글 " + i)
                    .content("{\"type\":\"doc\"}")
                    .contentText("내용")
                    .recommendCount(0L)
                    .viewCount(0L)
                    .commentCount(0L)
                    .build());
            postEntityTagRepository.save(PostEntityTag.builder()
                    .postId(post.getId())
                    .entityType(entityType)
                    .entityId(entityId)
                    .build());
        }
    }

    @Test
    void should_exclude_hidden_concert_tags_when_finding_trending_entity_tags() {
        // given
        Concert excludedConcert = saveConcert("제외된 공연", ConcertStatus.EXCLUDED);
        Concert pendingConcert = saveConcert("검토 대기 공연", ConcertStatus.PENDING);
        saveTaggedPosts(EntityType.CONCERT, excludedConcert.getId(), 2);
        saveTaggedPosts(EntityType.CONCERT, pendingConcert.getId(), 2);

        // when
        List<EntityTagCount> result = postEntityTagRepository.findTrendingEntityTags(since, HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result)
                .extracting(EntityTagCount::entityType, EntityTagCount::entityId)
                .doesNotContain(
                        tuple(EntityType.CONCERT, excludedConcert.getId()),
                        tuple(EntityType.CONCERT, pendingConcert.getId()));
    }

    @Test
    void should_fill_limit_with_public_entities_when_hidden_concert_has_most_mentions() {
        // given
        Concert hiddenConcert = saveConcert("언급 1위 숨김 공연", ConcertStatus.EXCLUDED);
        Concert publicConcert = saveConcert("아이유 HER 월드투어", ConcertStatus.UPCOMING);
        Artist artist = saveArtist("아이유");
        saveTaggedPosts(EntityType.CONCERT, hiddenConcert.getId(), 5);
        saveTaggedPosts(EntityType.CONCERT, publicConcert.getId(), 3);
        saveTaggedPosts(EntityType.ARTIST, artist.getId(), 2);

        // when
        List<EntityTagCount> result = postEntityTagRepository.findTrendingEntityTags(since, HIDDEN, PageRequest.of(0, 2));

        // then
        assertThat(result)
                .extracting(EntityTagCount::entityType, EntityTagCount::entityId)
                .containsExactly(
                        tuple(EntityType.CONCERT, publicConcert.getId()),
                        tuple(EntityType.ARTIST, artist.getId()));
    }

    @Test
    void should_count_public_concert_and_artist_tags_when_finding_trending_entity_tags() {
        // given
        Concert publicConcert = saveConcert("아이유 HER 월드투어", ConcertStatus.UPCOMING);
        Artist artist = saveArtist("아이유");
        saveTaggedPosts(EntityType.CONCERT, publicConcert.getId(), 3);
        saveTaggedPosts(EntityType.ARTIST, artist.getId(), 2);

        // when
        List<EntityTagCount> result = postEntityTagRepository.findTrendingEntityTags(since, HIDDEN, PageRequest.of(0, 20));

        // then
        assertThat(result)
                .extracting(EntityTagCount::entityType, EntityTagCount::entityId, EntityTagCount::count)
                .contains(
                        tuple(EntityType.CONCERT, publicConcert.getId(), 3L),
                        tuple(EntityType.ARTIST, artist.getId(), 2L));
    }

    private Post savePost(String title) {
        return postRepository.save(Post.builder()
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title(title)
                .content("{\"type\":\"doc\"}")
                .contentText("내용")
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .build());
    }

    private PostEntityTag saveTag(Long postId, EntityType entityType, Long entityId) {
        return postEntityTagRepository.save(PostEntityTag.builder()
                .postId(postId)
                .entityType(entityType)
                .entityId(entityId)
                .build());
    }

    @Test
    void should_return_tags_in_saved_order_when_finding_by_post_id() {
        // given
        Post post = savePost("본문 카드 순서 글");
        saveTag(post.getId(), EntityType.CONCERT, 30L);
        saveTag(post.getId(), EntityType.ARTIST, 5L);
        saveTag(post.getId(), EntityType.RELEASE, 12L);

        // when
        List<PostEntityTag> result = postEntityTagRepository.findByPostId(post.getId());

        // then
        assertThat(result)
                .extracting(PostEntityTag::getEntityType, PostEntityTag::getEntityId)
                .containsExactly(
                        tuple(EntityType.CONCERT, 30L),
                        tuple(EntityType.ARTIST, 5L),
                        tuple(EntityType.RELEASE, 12L));
    }

    @Test
    void should_return_tags_in_id_order_keeping_per_post_order_when_finding_by_post_ids() {
        // given
        Post postA = savePost("게시글 A");
        Post postB = savePost("게시글 B");
        Post otherPost = savePost("조회 대상 아닌 게시글");
        PostEntityTag a1 = saveTag(postA.getId(), EntityType.CONCERT, 30L);
        PostEntityTag b1 = saveTag(postB.getId(), EntityType.RELEASE, 12L);
        saveTag(otherPost.getId(), EntityType.ARTIST, 7L);
        PostEntityTag a2 = saveTag(postA.getId(), EntityType.ARTIST, 5L);
        PostEntityTag b2 = saveTag(postB.getId(), EntityType.CONCERT, 3L);

        // when
        List<PostEntityTag> result = postEntityTagRepository.findByPostIdIn(List.of(postA.getId(), postB.getId()));

        // then
        assertThat(result)
                .extracting(PostEntityTag::getId)
                .containsExactly(a1.getId(), b1.getId(), a2.getId(), b2.getId());
        assertThat(result)
                .filteredOn(tag -> tag.getPostId().equals(postA.getId()))
                .extracting(PostEntityTag::getEntityType, PostEntityTag::getEntityId)
                .containsExactly(tuple(EntityType.CONCERT, 30L), tuple(EntityType.ARTIST, 5L));
        assertThat(result)
                .filteredOn(tag -> tag.getPostId().equals(postB.getId()))
                .extracting(PostEntityTag::getEntityType, PostEntityTag::getEntityId)
                .containsExactly(tuple(EntityType.RELEASE, 12L), tuple(EntityType.CONCERT, 3L));
    }

    @Test
    void should_return_empty_list_when_finding_by_empty_post_ids() {
        // given
        Post post = savePost("태그 있는 글");
        saveTag(post.getId(), EntityType.ARTIST, 5L);

        // when
        List<PostEntityTag> result = postEntityTagRepository.findByPostIdIn(Collections.emptyList());

        // then
        assertThat(result).isEmpty();
    }
}
