package com.Coming.Backend.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.entity.PostEntityTag;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostEntityTagRepository postEntityTagRepository;

    @Autowired
    private ArtistRepository artistRepository;

    private static final Long AUTHOR_ID = 1L;

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

    @Test
    void should_return_post_when_title_matches_search_query() {
        // given
        Post post = postRepository.save(buildPost("아이유 단독 콘서트 후기", "정말 좋았다"));

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_return_post_when_content_text_matches_search_query() {
        // given
        Post post = postRepository.save(buildPost("공연 후기", "정말 재밌는 아이유 콘서트였다"));

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), PageRequest.of(0, 20));

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
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void should_return_empty_page_when_no_post_matches_search_query() {
        // given
        postRepository.save(buildPost("다른 제목", "다른 내용"));

        // when
        Page<Post> result = postRepository.searchPosts(likeQuery("존재하지않는검색어"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_return_posts_ordered_by_created_at_desc_when_multiple_posts_match() throws InterruptedException {
        // given
        Post olderPost = postRepository.save(buildPost("아이유 1번째 글", "내용"));
        Thread.sleep(10);
        Post newerPost = postRepository.save(buildPost("아이유 2번째 글", "내용"));

        // when
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> result = postRepository.searchPosts(likeQuery("아이유"), pageable);

        // then
        assertThat(result.getContent()).extracting(Post::getId)
                .containsSubsequence(newerPost.getId(), olderPost.getId());
    }
}
