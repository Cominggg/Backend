package com.Coming.Backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.dto.EntityTagRequest;
import com.Coming.Backend.post.dto.PostCreateRequest;
import com.Coming.Backend.post.dto.PostCreateResponse;
import com.Coming.Backend.post.dto.PostDetailResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.dto.PostUpdateRequest;
import com.Coming.Backend.post.dto.RecommendCountResponse;
import com.Coming.Backend.post.dto.TrendingTagResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.entity.PostEntityTag;
import com.Coming.Backend.post.entity.PostRecommend;
import com.Coming.Backend.post.exception.AlreadyRecommendedException;
import com.Coming.Backend.post.exception.NotRecommendedException;
import com.Coming.Backend.post.exception.PostContentTooLongException;
import com.Coming.Backend.post.exception.PostForbiddenException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.repository.EntityTagCount;
import com.Coming.Backend.post.repository.PostEntityTagRepository;
import com.Coming.Backend.post.repository.PostRecommendRepository;
import com.Coming.Backend.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostEntityTagRepository postEntityTagRepository;

    @Mock
    private PostRecommendRepository postRecommendRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityLookupService entityLookupService;

    private static final Long POST_ID = 10L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    /**
     * Spring이 실제로 @RequestBody를 역직렬화할 때 만드는 것과 동일한 형태(Map/List)의 Tiptap 문서.
     * "안녕하세요" 텍스트 노드 하나를 포함한다.
     */
    private Map<String, Object> sampleContent() {
        return Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "안녕하세요")
                        ))
                )
        );
    }

    /**
     * "a"를 length만큼 반복한 텍스트 노드 하나를 포함한 Tiptap 문서. contentText 길이를 정확히 통제하기 위해 사용한다.
     */
    private Map<String, Object> contentWithLength(int length) {
        return Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "a".repeat(length))
                        ))
                )
        );
    }

    private Post buildPost(Long id, Long userId, PostCategory category, String title, long viewCount) {
        return Post.builder()
                .id(id)
                .userId(userId)
                .category(category)
                .title(title)
                .content("{\"type\":\"doc\"}")
                .contentText("기존 텍스트")
                .recommendCount(0L)
                .viewCount(viewCount)
                .commentCount(0L)
                .build();
    }

    private User buildUser(Long id, String nickname) {
        return User.builder().id(id).nickname(nickname).build();
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void should_create_post_when_review_category_given_without_entity_tags() {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", sampleContent(), null);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_create_post_when_info_category_given_without_entity_tags() {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.INFO, "정보 제목", sampleContent(), List.of());
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_create_post_when_free_category_given_without_entity_tags() {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", sampleContent(), null);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).contains("\"type\":\"doc\"");
        assertThat(captor.getValue().getContentText()).isEqualTo("안녕하세요");
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_save_entity_tags_when_review_category_given_with_entity_tags() {
        // given
        List<EntityTagRequest> tags = List.of(
                new EntityTagRequest(EntityType.ARTIST, 1L),
                new EntityTagRequest(EntityType.CONCERT, 2L)
        );
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", sampleContent(), tags);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        verify(postEntityTagRepository, times(2)).save(any(PostEntityTag.class));
    }

    @Test
    void should_create_post_when_content_text_length_is_exactly_max_length() {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "제목", contentWithLength(10000), null);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void should_throw_content_too_long_exception_when_content_text_length_exceeds_max_length_on_create() {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "제목", contentWithLength(10001), null);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(PostContentTooLongException.class)
                .hasMessage(ErrorCode.POST_CONTENT_TOO_LONG.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    // -------------------------------------------------------------------------
    // getDetail
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_post_does_not_exist() {
        // given
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getDetail(POST_ID, AUTHOR_ID))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_increment_view_count_and_return_incremented_view_count_when_post_found() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 5L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        verify(postRepository).incrementViewCount(POST_ID);
        assertThat(response.viewCount()).isEqualTo(6L);
    }

    @Test
    void should_return_null_recommended_and_false_author_when_user_id_not_given() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        assertThat(response.isRecommended()).isNull();
        assertThat(response.isAuthor()).isFalse();
        verify(postRecommendRepository, never()).existsByUserIdAndPostId(any(), any());
    }

    @Test
    void should_return_true_author_when_user_id_matches_post_author() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));
        given(postRecommendRepository.existsByUserIdAndPostId(AUTHOR_ID, POST_ID)).willReturn(false);

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, AUTHOR_ID);

        // then
        assertThat(response.isAuthor()).isTrue();
    }

    @Test
    void should_return_true_recommended_when_user_has_recommended_post() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));
        given(postRecommendRepository.existsByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(true);

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, OTHER_USER_ID);

        // then
        assertThat(response.isRecommended()).isTrue();
    }

    @Test
    void should_return_false_recommended_when_user_has_not_recommended_post() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));
        given(postRecommendRepository.existsByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(false);

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, OTHER_USER_ID);

        // then
        assertThat(response.isRecommended()).isFalse();
    }

    @Test
    void should_map_entity_tags_using_entity_lookup_service_when_post_found() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        PostEntityTag tag = PostEntityTag.builder()
                .postId(POST_ID)
                .entityType(EntityType.ARTIST)
                .entityId(1L)
                .build();
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, 1L, "IU", null, "https://image.example.com/iu.jpg");
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.ARTIST, 1L);

        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(tag));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of(key, card));
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        assertThat(response.entityTags()).hasSize(1);
        assertThat(response.entityTags().get(0).entityType()).isEqualTo(EntityType.ARTIST);
        assertThat(response.entityTags().get(0).entityId()).isEqualTo(1L);
        assertThat(response.entityTags().get(0).title()).isEqualTo("IU");
    }

    // -------------------------------------------------------------------------
    // getList
    // -------------------------------------------------------------------------

    @Test
    void should_return_page_response_when_getting_list() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 3L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
        given(postRepository.findPosts(isNull(), eq(pageable))).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getList(null, 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("제목");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // getBacklinks
    // -------------------------------------------------------------------------

    @Test
    void should_return_page_response_sorted_by_recommend_count_when_sort_is_recommend() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 3L);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "recommendCount"));
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
        given(postRepository.findByEntityTag(eq(EntityType.ARTIST), eq(1L), eq(pageable))).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getBacklinks(EntityType.ARTIST, 1L, "recommend", 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("제목");
        verify(postRepository).findByEntityTag(EntityType.ARTIST, 1L, pageable);
    }

    @Test
    void should_return_page_response_sorted_by_created_at_when_sort_is_latest() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 3L);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
        given(postRepository.findByEntityTag(eq(EntityType.CONCERT), eq(2L), eq(pageable))).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getBacklinks(EntityType.CONCERT, 2L, "latest", 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        verify(postRepository).findByEntityTag(EntityType.CONCERT, 2L, pageable);
    }

    @Test
    void should_throw_invalid_input_exception_when_sort_is_not_allowed_value() {
        // when & then
        assertThatThrownBy(() -> postService.getBacklinks(EntityType.ARTIST, 1L, "oldest", 0, 20))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
        verify(postRepository, never()).findByEntityTag(any(), any(), any());
    }

    @Test
    void should_return_empty_content_when_no_backlinks_found() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        given(postRepository.findByEntityTag(eq(EntityType.RELEASE), eq(3L), eq(pageable))).willReturn(Page.empty(pageable));

        // when
        PageResponse<PostSummaryResponse> response = postService.getBacklinks(EntityType.RELEASE, 3L, "latest", 0, 20);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    // -------------------------------------------------------------------------
    // getPopular
    // -------------------------------------------------------------------------

    @Test
    void should_return_summary_responses_when_popular_posts_found() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "인기 게시글", 10L);
        Pageable pageable = PageRequest.of(0, 5);
        given(postRepository.findPopularPosts(any(LocalDateTime.class), eq(pageable))).willReturn(List.of(post));
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        List<PostSummaryResponse> response = postService.getPopular(7, 5);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("인기 게시글");
    }

    @Test
    void should_return_empty_list_when_no_popular_posts_found() {
        // given
        Pageable pageable = PageRequest.of(0, 5);
        given(postRepository.findPopularPosts(any(LocalDateTime.class), eq(pageable))).willReturn(List.of());

        // when
        List<PostSummaryResponse> response = postService.getPopular(7, 5);

        // then
        assertThat(response).isEmpty();
        verify(postEntityTagRepository, never()).findByPostIdIn(any());
    }

    // -------------------------------------------------------------------------
    // getTrendingTags
    // -------------------------------------------------------------------------

    @Test
    void should_return_trending_tags_when_entity_tags_found() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        EntityTagCount count = new EntityTagCount(EntityType.ARTIST, 1L, 3L);
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.ARTIST, 1L);
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, 1L, "IU", null, "https://image.example.com/iu.jpg");
        given(postEntityTagRepository.findTrendingEntityTags(any(LocalDateTime.class), eq(pageable))).willReturn(List.of(count));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of(key, card));

        // when
        List<TrendingTagResponse> response = postService.getTrendingTags(7, 10);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).entityType()).isEqualTo(EntityType.ARTIST);
        assertThat(response.get(0).entityId()).isEqualTo(1L);
        assertThat(response.get(0).title()).isEqualTo("IU");
        assertThat(response.get(0).count()).isEqualTo(3L);
    }

    @Test
    void should_return_empty_list_when_no_trending_tags_found() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(postEntityTagRepository.findTrendingEntityTags(any(LocalDateTime.class), eq(pageable))).willReturn(List.of());

        // when
        List<TrendingTagResponse> response = postService.getTrendingTags(7, 10);

        // then
        assertThat(response).isEmpty();
        verify(entityLookupService, never()).findCards(any());
    }

    @Test
    void should_filter_out_trending_tag_when_entity_reference_is_deleted() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        EntityTagCount aliveCount = new EntityTagCount(EntityType.ARTIST, 1L, 3L);
        EntityTagCount deletedCount = new EntityTagCount(EntityType.CONCERT, 2L, 2L);
        EntityLookupService.EntityKey aliveKey = new EntityLookupService.EntityKey(EntityType.ARTIST, 1L);
        EntityLookupService.EntityKey deletedKey = new EntityLookupService.EntityKey(EntityType.CONCERT, 2L);
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, 1L, "IU", null, "https://image.example.com/iu.jpg");
        given(postEntityTagRepository.findTrendingEntityTags(any(LocalDateTime.class), eq(pageable)))
                .willReturn(List.of(aliveCount, deletedCount));
        given(entityLookupService.findCards(List.of(aliveKey, deletedKey))).willReturn(Map.of(aliveKey, card));

        // when
        List<TrendingTagResponse> response = postService.getTrendingTags(7, 10);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).entityId()).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // search
    // -------------------------------------------------------------------------

    @Test
    void should_return_page_response_when_title_matches_search_query() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "IU 콘서트 후기", 3L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
        given(postRepository.searchPosts(eq("%iu%"), eq(pageable))).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.search("IU", 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("IU 콘서트 후기");
        verify(postRepository).searchPosts("%iu%", pageable);
    }

    @Test
    void should_return_empty_content_when_no_search_result_found() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(postRepository.searchPosts(eq("%없는검색어%"), eq(pageable))).willReturn(Page.empty(pageable));

        // when
        PageResponse<PostSummaryResponse> response = postService.search("없는검색어", 0, 20);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_updating_post_that_does_not_exist() {
        // given
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, null);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_post_forbidden_exception_when_updater_is_not_author() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, null);

        // when & then
        assertThatThrownBy(() -> postService.update(OTHER_USER_ID, POST_ID, request))
                .isInstanceOf(PostForbiddenException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void should_update_title_and_content_when_given_in_request() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "기존 제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", sampleContent(), null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).contains("\"type\":\"doc\"");
        assertThat(post.getContentText()).isEqualTo("안녕하세요");
    }

    @Test
    void should_keep_existing_title_and_content_when_null_in_request() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "기존 제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getContent()).isEqualTo("{\"type\":\"doc\"}");
    }

    @Test
    void should_not_touch_entity_tags_when_entity_tags_is_null_in_request() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        verify(postEntityTagRepository, never()).deleteByPostId(POST_ID);
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_replace_entity_tags_when_entity_tags_given_in_request() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        List<EntityTagRequest> newTags = List.of(
                new EntityTagRequest(EntityType.ARTIST, 1L),
                new EntityTagRequest(EntityType.CONCERT, 2L)
        );
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, newTags);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        verify(postEntityTagRepository).deleteByPostId(POST_ID);
        verify(postEntityTagRepository, times(2)).save(any(PostEntityTag.class));
    }

    @Test
    void should_update_category_when_review_category_given_without_entity_tags() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(PostCategory.REVIEW, null, null, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getCategory()).isEqualTo(PostCategory.REVIEW);
    }

    @Test
    void should_update_content_when_content_text_length_is_exactly_max_length() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "기존 제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, null, contentWithLength(10000), null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getContentText()).hasSize(10000);
    }

    @Test
    void should_throw_content_too_long_exception_and_keep_existing_fields_when_content_text_length_exceeds_max_length_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "기존 제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", contentWithLength(10001), null);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostContentTooLongException.class)
                .hasMessage(ErrorCode.POST_CONTENT_TOO_LONG.getMessage());
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getContent()).isEqualTo("{\"type\":\"doc\"}");
        assertThat(post.getContentText()).isEqualTo("기존 텍스트");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_deleting_post_that_does_not_exist() {
        // given
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.delete(AUTHOR_ID, POST_ID))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_post_forbidden_exception_when_deleter_is_not_author() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(() -> postService.delete(OTHER_USER_ID, POST_ID))
                .isInstanceOf(PostForbiddenException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void should_delete_post_and_entity_tags_when_author_deletes() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        // when
        postService.delete(AUTHOR_ID, POST_ID);

        // then
        verify(postEntityTagRepository).deleteByPostId(POST_ID);
        verify(postRepository).delete(post);
    }

    // -------------------------------------------------------------------------
    // recommend
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_recommending_post_that_does_not_exist() {
        // given
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.recommend(AUTHOR_ID, POST_ID))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_already_recommended_exception_when_user_already_recommended_post() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRecommendRepository.existsByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> postService.recommend(OTHER_USER_ID, POST_ID))
                .isInstanceOf(AlreadyRecommendedException.class)
                .hasMessage(ErrorCode.ALREADY_RECOMMENDED.getMessage());
        verify(postRecommendRepository, never()).save(any(PostRecommend.class));
    }

    @Test
    void should_throw_already_recommended_exception_when_save_violates_unique_constraint() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRecommendRepository.existsByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(false);
        given(postRecommendRepository.save(any(PostRecommend.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> postService.recommend(OTHER_USER_ID, POST_ID))
                .isInstanceOf(AlreadyRecommendedException.class)
                .hasMessage(ErrorCode.ALREADY_RECOMMENDED.getMessage());
        verify(postRepository, never()).incrementRecommendCount(POST_ID);
    }

    @Test
    void should_save_recommend_and_return_incremented_count_when_user_has_not_recommended_post() {
        // given
        Post post = Post.builder()
                .id(POST_ID)
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title("제목")
                .recommendCount(3L)
                .build();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRecommendRepository.existsByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(false);

        // when
        RecommendCountResponse response = postService.recommend(OTHER_USER_ID, POST_ID);

        // then
        assertThat(response.recommendCount()).isEqualTo(4L);
        verify(postRecommendRepository).save(any(PostRecommend.class));
        verify(postRepository).incrementRecommendCount(POST_ID);
    }

    // -------------------------------------------------------------------------
    // unrecommend
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_unrecommending_post_that_does_not_exist() {
        // given
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.unrecommend(AUTHOR_ID, POST_ID))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_not_recommended_exception_when_user_has_not_recommended_post() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRecommendRepository.findByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.unrecommend(OTHER_USER_ID, POST_ID))
                .isInstanceOf(NotRecommendedException.class)
                .hasMessage(ErrorCode.NOT_RECOMMENDED.getMessage());
        verify(postRepository, never()).decrementRecommendCount(POST_ID);
    }

    @Test
    void should_delete_recommend_and_return_decremented_count_when_user_has_recommended_post() {
        // given
        Post post = Post.builder()
                .id(POST_ID)
                .userId(AUTHOR_ID)
                .category(PostCategory.FREE)
                .title("제목")
                .recommendCount(3L)
                .build();
        PostRecommend recommend = PostRecommend.builder()
                .userId(OTHER_USER_ID)
                .postId(POST_ID)
                .build();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRecommendRepository.findByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(Optional.of(recommend));

        // when
        RecommendCountResponse response = postService.unrecommend(OTHER_USER_ID, POST_ID);

        // then
        assertThat(response.recommendCount()).isEqualTo(2L);
        verify(postRecommendRepository).delete(recommend);
        verify(postRepository).decrementRecommendCount(POST_ID);
    }
}
