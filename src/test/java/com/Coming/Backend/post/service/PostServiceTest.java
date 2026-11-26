package com.Coming.Backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.auth.service.UserStatusValidator;
import com.Coming.Backend.common.exception.ContentInvalidFormatException;
import com.Coming.Backend.common.exception.ContentTooLongException;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.post.dto.EntityCardResponse;
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
import com.Coming.Backend.post.exception.PostConcertTagHiddenException;
import com.Coming.Backend.post.exception.PostCoverConcertNotTaggedException;
import com.Coming.Backend.post.exception.PostForbiddenException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.repository.CommentLikeRepository;
import com.Coming.Backend.post.repository.CommentRepository;
import com.Coming.Backend.post.repository.EntityTagCount;
import com.Coming.Backend.post.repository.PostEntityTagRepository;
import com.Coming.Backend.post.repository.PostRecommendRepository;
import com.Coming.Backend.post.repository.PostRepository;
import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityLookupService entityLookupService;

    @Mock
    private UserStatusValidator userStatusValidator;

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

    /**
     * 텍스트 노드는 짧지만(contentText 검증 통과), 링크 href를 길게 채워 직렬화된 JSON 문자열 자체는
     * 크게 만든 Tiptap 문서. content 원본 크기 상한 검증용.
     * 허용 밖 attrs는 저장 전에 지워지므로 허용된 속성(link.href)으로 크기를 키운다.
     */
    private Map<String, Object> contentWithLargeStructureAndShortText(int paddingLength) {
        return Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "안녕하세요", "marks", List.of(
                                        Map.of("type", "link", "attrs",
                                                Map.of("href", "https://example.com/" + "x".repeat(paddingLength)))
                                ))
                        ))
                )
        );
    }

    /**
     * 본문 언급 카드 노드. 카드는 최상위 블록에만 둘 수 있다.
     */
    private Map<String, Object> card(EntityType entityType, Long entityId) {
        return Map.of("type", "entityMentionCard", "attrs",
                Map.of("entityType", entityType.name(), "entityId", entityId, "title", "카드 제목"));
    }

    /**
     * "안녕하세요" 문단 뒤에 주어진 카드를 순서대로 둔 Tiptap 문서.
     */
    private Map<String, Object> contentWithCards(Map<?, ?>... cards) {
        List<Object> nodes = new ArrayList<>();
        nodes.add(Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", "안녕하세요"))));
        nodes.addAll(List.of(cards));
        return Map.of("type", "doc", "content", nodes);
    }

    /**
     * 허용 목록에 없는 노드(image)를 포함한 Tiptap 문서. 서식 위반 검증용.
     */
    private Map<String, Object> contentWithDisallowedNode() {
        return Map.of("type", "doc", "content", List.of(
                Map.of("type", "image", "attrs", Map.of("src", "https://example.com/a.png"))
        ));
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

    private Post buildPostWithCover(Long coverConcertId) {
        return Post.builder()
                .id(POST_ID)
                .userId(AUTHOR_ID)
                .category(PostCategory.REVIEW)
                .title("제목")
                .content("{\"type\":\"doc\"}")
                .contentText("기존 텍스트")
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .coverConcertId(coverConcertId)
                .build();
    }

    private EntityCardResponse buildConcertCard(Long concertId) {
        return new EntityCardResponse(EntityType.CONCERT, concertId, "IU 콘서트 HEREH", null, null, null);
    }

    private User buildUser(Long id, String nickname) {
        return User.builder().id(id).nickname(nickname).build();
    }

    private PostEntityTag buildConcertTag(Long concertId) {
        return PostEntityTag.builder()
                .postId(POST_ID)
                .entityType(EntityType.CONCERT)
                .entityId(concertId)
                .build();
    }

    private void stubPostSaveAssigningId() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });
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
        PostCreateRequest request = new PostCreateRequest(PostCategory.INFO, "정보 제목", sampleContent(), null);
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
    void should_save_entity_tags_when_review_content_has_mention_cards() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.ARTIST, 1L), card(EntityType.CONCERT, 2L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
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
    void should_deduplicate_entity_tags_when_duplicate_cards_given_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.ARTIST, 1L), card(EntityType.ARTIST, 1L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", POST_ID);
            return saved;
        });

        // when
        postService.create(AUTHOR_ID, request);

        // then
        verify(postEntityTagRepository, times(1)).save(any(PostEntityTag.class));
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
                .isInstanceOf(ContentTooLongException.class)
                .hasMessage(ErrorCode.CONTENT_TOO_LONG.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void should_throw_content_too_long_exception_when_raw_content_size_exceeds_limit_despite_short_text_on_create() {
        // given: contentText는 10000자 제한을 통과할 만큼 짧지만, 구조(attrs)만 방대해 직렬화된 content가 50000자를 초과한다.
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "제목",
                contentWithLargeStructureAndShortText(60000), null);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(ContentTooLongException.class)
                .hasMessage(ErrorCode.CONTENT_TOO_LONG.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void should_throw_concert_tag_hidden_exception_and_not_save_when_excluded_concert_tagged_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        given(entityLookupService.hasHiddenConcert(Set.of(100L, 200L))).willReturn(true);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(PostConcertTagHiddenException.class)
                .hasMessage(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage());
        verify(postRepository, never()).save(any(Post.class));
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_throw_concert_tag_hidden_exception_and_not_save_when_pending_concert_tagged_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        given(entityLookupService.hasHiddenConcert(Set.of(100L))).willReturn(true);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(PostConcertTagHiddenException.class)
                .hasMessage(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void should_create_post_when_only_public_concerts_tagged() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        given(entityLookupService.hasHiddenConcert(Set.of(100L, 200L))).willReturn(false);
        stubPostSaveAssigningId();

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        verify(postEntityTagRepository, times(2)).save(any(PostEntityTag.class));
    }

    @Test
    void should_create_post_when_tagged_concert_does_not_exist() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 999L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        given(entityLookupService.hasHiddenConcert(Set.of(999L))).willReturn(false);
        stubPostSaveAssigningId();

        // when
        PostCreateResponse response = postService.create(AUTHOR_ID, request);

        // then
        assertThat(response.id()).isEqualTo(POST_ID);
        verify(postEntityTagRepository).save(any(PostEntityTag.class));
    }

    @Test
    void should_check_hidden_concerts_with_empty_set_when_no_concert_tag_given_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.ARTIST, 1L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        stubPostSaveAssigningId();

        // when
        postService.create(AUTHOR_ID, request);

        // then
        verify(entityLookupService).hasHiddenConcert(Set.of());
    }

    @Test
    void should_save_cover_concert_id_when_tagged_concert_given_as_cover_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, 200L);
        given(entityLookupService.hasHiddenConcert(Set.of(100L, 200L))).willReturn(false);
        stubPostSaveAssigningId();

        // when
        postService.create(AUTHOR_ID, request);

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getCoverConcertId()).isEqualTo(200L);
    }

    @Test
    void should_throw_cover_concert_not_tagged_exception_and_not_save_when_cover_concert_not_in_content_cards_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, 300L);
        given(entityLookupService.hasHiddenConcert(Set.of(100L))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(PostCoverConcertNotTaggedException.class)
                .hasMessage(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.getMessage());
        verify(postRepository, never()).save(any(Post.class));
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_throw_cover_concert_not_tagged_exception_when_cover_id_matches_only_non_concert_card_on_create() {
        // given: 같은 id 300이 아티스트 카드로만 있다
        Map<String, Object> content = contentWithCards(card(EntityType.ARTIST, 300L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, 300L);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(PostCoverConcertNotTaggedException.class)
                .hasMessage(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void should_save_null_cover_concert_id_when_cover_not_given_on_create() {
        // given
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        given(entityLookupService.hasHiddenConcert(Set.of(100L))).willReturn(false);
        stubPostSaveAssigningId();

        // when
        postService.create(AUTHOR_ID, request);

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getCoverConcertId()).isNull();
    }

    @Test
    void should_save_entity_tags_in_card_order_when_content_has_mention_cards_on_create() {
        // given
        Map<String, Object> content = contentWithCards(
                card(EntityType.CONCERT, 200L), card(EntityType.ARTIST, 1L), card(EntityType.CONCERT, 100L));
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", content, null);
        stubPostSaveAssigningId();

        // when
        postService.create(AUTHOR_ID, request);

        // then
        ArgumentCaptor<PostEntityTag> captor = ArgumentCaptor.forClass(PostEntityTag.class);
        verify(postEntityTagRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PostEntityTag::getPostId, PostEntityTag::getEntityType, PostEntityTag::getEntityId)
                .containsExactly(
                        tuple(POST_ID, EntityType.CONCERT, 200L),
                        tuple(POST_ID, EntityType.ARTIST, 1L),
                        tuple(POST_ID, EntityType.CONCERT, 100L));
    }

    @Test
    void should_save_content_without_disallowed_attrs_when_content_has_paragraph_text_align_on_create() {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "attrs", Map.of("textAlign", "center"), "content", List.of(
                        Map.of("type", "text", "text", "안녕하세요")
                ))
        ));
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", content, null);
        stubPostSaveAssigningId();

        // when
        postService.create(AUTHOR_ID, request);

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"안녕하세요\"}]}]}");
    }

    @Test
    void should_throw_content_invalid_format_exception_and_not_save_when_content_has_disallowed_node_on_create() {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", contentWithDisallowedNode(), null);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, request))
                .isInstanceOf(ContentInvalidFormatException.class)
                .hasMessage(ErrorCode.CONTENT_INVALID_FORMAT.getMessage());
        verify(postRepository, never()).save(any(Post.class));
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
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
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, 1L, "IU", null, "https://image.example.com/iu.jpg", null);
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

    @Test
    void should_include_release_group_id_when_entity_tag_is_track() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        PostEntityTag tag = PostEntityTag.builder()
                .postId(POST_ID)
                .entityType(EntityType.TRACK)
                .entityId(200L)
                .build();
        EntityCardResponse card = new EntityCardResponse(EntityType.TRACK, 200L, "라일락", "IU · LILAC", null, 100L);
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.TRACK, 200L);

        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(tag));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of(key, card));
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        assertThat(response.entityTags()).hasSize(1);
        assertThat(response.entityTags().get(0).entityType()).isEqualTo(EntityType.TRACK);
        assertThat(response.entityTags().get(0).title()).isEqualTo("라일락");
        assertThat(response.entityTags().get(0).releaseGroupId()).isEqualTo(100L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_remove_hidden_and_missing_concert_mentions_and_keep_public_one_when_getting_detail() {
        // given: 100은 숨김, 200은 공개, 300은 존재하지 않는 공연
        String content = """
                {"type":"doc","content":[
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":100}},
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":200}},
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":300}}
                ]}""";
        Post post = Post.builder()
                .id(POST_ID)
                .userId(AUTHOR_ID)
                .category(PostCategory.REVIEW)
                .title("제목")
                .content(content)
                .contentText("")
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .build();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));
        given(entityLookupService.findVisibleConcertIds(Set.of(100L, 200L, 300L))).willReturn(Set.of(200L));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) ((Map<String, Object>) response.content()).get("content");
        assertThat(nodes)
                .extracting(node -> ((Map<String, Object>) node.get("attrs")).get("entityId"))
                .containsExactly(200);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_remove_concert_mentions_with_null_missing_or_string_id_when_getting_detail() {
        // given: 유효한 공연 id가 하나도 없는 멘션만 있는 본문
        String content = """
                {"type":"doc","content":[
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":null}},
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT"}},
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":"abc"}},
                  {"type":"paragraph","content":[{"type":"text","text":"본문"}]}
                ]}""";
        Post post = Post.builder()
                .id(POST_ID)
                .userId(AUTHOR_ID)
                .category(PostCategory.REVIEW)
                .title("제목")
                .content(content)
                .contentText("")
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .build();
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) ((Map<String, Object>) response.content()).get("content");
        assertThat(nodes).extracting(node -> node.get("type")).containsExactly("paragraph");
        verify(entityLookupService, never()).findVisibleConcertIds(any());
    }

    @Test
    void should_not_query_concerts_when_content_has_no_concert_mention_on_get_detail() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        postService.getDetail(POST_ID, null);

        // then
        verify(entityLookupService, never()).findVisibleConcertIds(any());
    }

    @Test
    void should_check_visibility_only_for_untagged_mentions_when_tagged_concert_mentioned_on_get_detail() {
        // given: 100은 응답 태그에 있는 공개 공연, 200은 태그에 없는 공연
        String content = """
                {"type":"doc","content":[
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":100}},
                  {"type":"entityMentionCard","attrs":{"entityType":"CONCERT","entityId":200}}
                ]}""";
        Post post = Post.builder()
                .id(POST_ID)
                .userId(AUTHOR_ID)
                .category(PostCategory.REVIEW)
                .title("제목")
                .content(content)
                .contentText("")
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .build();
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.CONCERT, 100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of(key, buildConcertCard(100L)));
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));
        given(entityLookupService.findVisibleConcertIds(Set.of(200L))).willReturn(Set.of(200L));

        // when
        postService.getDetail(POST_ID, null);

        // then
        verify(entityLookupService).findVisibleConcertIds(Set.of(200L));
    }


    @Test
    void should_return_cover_concert_id_when_cover_concert_is_in_visible_tags_on_get_detail() {
        // given
        Post post = buildPostWithCover(100L);
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.CONCERT, 100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of(key, buildConcertCard(100L)));
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        assertThat(response.coverConcertId()).isEqualTo(100L);
    }

    @Test
    void should_return_null_cover_concert_id_and_keep_stored_value_when_cover_concert_excluded_from_tags_on_get_detail() {
        // given: 대표 공연 100이 숨김·삭제되어 EntityLookupService가 카드를 돌려주지 않는다
        Post post = buildPostWithCover(100L);
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.CONCERT, 100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of());
        given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PostDetailResponse response = postService.getDetail(POST_ID, null);

        // then
        assertThat(response.entityTags()).isEmpty();
        assertThat(response.coverConcertId()).isNull();
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
    }

    @Test
    void should_throw_user_suspended_exception_and_not_save_when_suspended_user_creates_post() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(AUTHOR_ID);

        // when & then
        assertThatThrownBy(() -> postService.create(AUTHOR_ID, null))
                .isInstanceOf(UserSuspendedException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void should_throw_user_suspended_exception_when_suspended_user_updates_post() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(AUTHOR_ID);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, null))
                .isInstanceOf(UserSuspendedException.class);
        verify(postRepository, never()).findById(any());
    }

    @Test
    void should_throw_user_suspended_exception_when_suspended_user_recommends_post() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(AUTHOR_ID);

        // when & then
        assertThatThrownBy(() -> postService.recommend(AUTHOR_ID, POST_ID))
                .isInstanceOf(UserSuspendedException.class);
        verify(postRecommendRepository, never()).save(any());
    }

    @Test
    void should_throw_user_suspended_exception_and_not_delete_when_suspended_user_deletes_post() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(AUTHOR_ID);

        // when & then
        assertThatThrownBy(() -> postService.delete(AUTHOR_ID, POST_ID))
                .isInstanceOf(UserSuspendedException.class);
        verify(postRepository, never()).delete(any());
    }

    @Test
    void should_throw_user_suspended_exception_and_not_decrement_when_suspended_user_unrecommends_post() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(AUTHOR_ID);

        // when & then
        assertThatThrownBy(() -> postService.unrecommend(AUTHOR_ID, POST_ID))
                .isInstanceOf(UserSuspendedException.class);
        verify(postRecommendRepository, never()).deleteByUserIdAndPostId(any(), any());
        verify(postRepository, never()).decrementRecommendCount(any());
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

    @Test
    void should_return_null_author_nickname_when_author_has_withdrawn() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 3L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
        given(postRepository.findPosts(isNull(), eq(pageable))).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, null)));

        // when
        PageResponse<PostSummaryResponse> response = postService.getList(null, 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).authorNickname()).isNull();
    }

    @Test
    void should_return_cover_concert_id_when_cover_concert_is_in_visible_tags_on_get_list() {
        // given
        Post post = buildPostWithCover(100L);
        Pageable pageable = PageRequest.of(0, 20);
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.CONCERT, 100L);
        given(postRepository.findPosts(isNull(), eq(pageable))).willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of(key, buildConcertCard(100L)));
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getList(null, 0, 20);

        // then
        assertThat(response.content().get(0).coverConcertId()).isEqualTo(100L);
    }

    @Test
    void should_return_null_cover_concert_id_when_cover_concert_excluded_from_tags_on_get_list() {
        // given: 대표 공연 100이 숨김·삭제되어 EntityLookupService가 카드를 돌려주지 않는다
        Post post = buildPostWithCover(100L);
        Pageable pageable = PageRequest.of(0, 20);
        EntityLookupService.EntityKey key = new EntityLookupService.EntityKey(EntityType.CONCERT, 100L);
        given(postRepository.findPosts(isNull(), eq(pageable))).willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.findCards(List.of(key))).willReturn(Map.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getList(null, 0, 20);

        // then
        assertThat(response.content().get(0).coverConcertId()).isNull();
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
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

    @Test
    void should_return_empty_page_without_querying_posts_when_backlink_concert_is_hidden() {
        // given
        given(entityLookupService.hasHiddenConcert(Set.of(2L))).willReturn(true);

        // when
        PageResponse<PostSummaryResponse> response = postService.getBacklinks(EntityType.CONCERT, 2L, "latest", 1, 10);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        verify(postRepository, never()).findByEntityTag(any(), any(), any());
    }

    @Test
    void should_query_backlinks_when_backlink_concert_is_public() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 3L);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        given(entityLookupService.hasHiddenConcert(Set.of(2L))).willReturn(false);
        given(postRepository.findByEntityTag(EntityType.CONCERT, 2L, pageable))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getBacklinks(EntityType.CONCERT, 2L, "latest", 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        verify(postRepository).findByEntityTag(EntityType.CONCERT, 2L, pageable);
    }

    @Test
    void should_not_check_hidden_concert_when_backlink_entity_type_is_artist() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        given(postRepository.findByEntityTag(EntityType.ARTIST, 1L, pageable)).willReturn(Page.empty(pageable));

        // when
        postService.getBacklinks(EntityType.ARTIST, 1L, "latest", 0, 20);

        // then
        verify(entityLookupService, never()).hasHiddenConcert(any());
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
    // getPopularBoard
    // -------------------------------------------------------------------------

    @Test
    void should_return_page_response_when_popular_board_posts_found() {
        // given
        ReflectionTestUtils.setField(postService, "popularBoardThreshold", 10L);
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "인기글", 3L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);
        given(postRepository.findPopularBoard(10L, pageable)).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.getPopularBoard(0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("인기글");
        verify(postRepository).findPopularBoard(10L, pageable);
    }

    @Test
    void should_return_empty_page_response_when_no_popular_board_posts_found() {
        // given
        ReflectionTestUtils.setField(postService, "popularBoardThreshold", 10L);
        Pageable pageable = PageRequest.of(0, 20);
        given(postRepository.findPopularBoard(10L, pageable)).willReturn(Page.empty(pageable));

        // when
        PageResponse<PostSummaryResponse> response = postService.getPopularBoard(0, 20);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
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
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, 1L, "IU", null, "https://image.example.com/iu.jpg", null);
        given(postEntityTagRepository.findTrendingEntityTags(any(LocalDateTime.class), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable))).willReturn(List.of(count));
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
        given(postEntityTagRepository.findTrendingEntityTags(any(LocalDateTime.class), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable))).willReturn(List.of());

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
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, 1L, "IU", null, "https://image.example.com/iu.jpg", null);
        given(postEntityTagRepository.findTrendingEntityTags(any(LocalDateTime.class), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable)))
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
        given(postRepository.searchPosts(eq("%iu%"), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable))).willReturn(page);
        given(postEntityTagRepository.findByPostIdIn(List.of(POST_ID))).willReturn(List.of());
        given(userRepository.findAllByIdIn(Set.of(AUTHOR_ID))).willReturn(List.of(buildUser(AUTHOR_ID, "IU")));

        // when
        PageResponse<PostSummaryResponse> response = postService.search("IU", 0, 20);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("IU 콘서트 후기");
        verify(postRepository).searchPosts(eq("%iu%"), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable));
    }

    @Test
    void should_return_empty_content_when_no_search_result_found() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(postRepository.searchPosts(eq("%없는검색어%"), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable))).willReturn(Page.empty(pageable));

        // when
        PageResponse<PostSummaryResponse> response = postService.search("없는검색어", 0, 20);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void should_escape_like_wildcards_when_search_query_contains_percent_and_underscore() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(postRepository.searchPosts(eq("%50\\%\\_off%"), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable))).willReturn(Page.empty(pageable));

        // when
        postService.search("50%_off", 0, 20);

        // then
        verify(postRepository).searchPosts(eq("%50\\%\\_off%"), eq(EnumSet.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING)), eq(pageable));
    }

    @Test
    void should_throw_invalid_input_exception_when_search_query_length_is_less_than_two() {
        // when & then
        assertThatThrownBy(() -> postService.search("a", 0, 20))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
        verify(postRepository, never()).searchPosts(any(), any(), any());
    }

    @Test
    void should_throw_invalid_input_exception_when_trimmed_search_query_length_is_less_than_two() {
        // when & then
        assertThatThrownBy(() -> postService.search("  a  ", 0, 20))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
        verify(postRepository, never()).searchPosts(any(), any(), any());
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
    void should_not_replace_entity_tags_when_content_is_null_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", null, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        verify(postEntityTagRepository, never()).deleteByPostId(POST_ID);
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_replace_entity_tags_with_mention_cards_when_content_given_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        Map<String, Object> content = contentWithCards(card(EntityType.ARTIST, 1L), card(EntityType.CONCERT, 2L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);

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
                .isInstanceOf(ContentTooLongException.class)
                .hasMessage(ErrorCode.CONTENT_TOO_LONG.getMessage());
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getContent()).isEqualTo("{\"type\":\"doc\"}");
        assertThat(post.getContentText()).isEqualTo("기존 텍스트");
    }

    @Test
    void should_throw_concert_tag_hidden_exception_and_not_replace_tags_when_newly_added_concert_is_hidden() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(200L))).willReturn(true);
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostConcertTagHiddenException.class)
                .hasMessage(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage());
        verify(postEntityTagRepository, never()).deleteByPostId(any());
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
    }

    @Test
    void should_replace_tags_and_check_hidden_concerts_with_empty_set_when_already_stored_hidden_concert_tag_resent() {
        // given: 100은 저장 이후 숨김 처리된 공연이지만 이미 저장된 태그라 본문에 다시 와도 검사 대상이 아니다
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        PostUpdateRequest request = new PostUpdateRequest(null, null,
                contentWithCards(card(EntityType.CONCERT, 100L)), null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        verify(entityLookupService).hasHiddenConcert(Set.of());
        verify(postEntityTagRepository).deleteByPostId(POST_ID);
        verify(postEntityTagRepository).save(any(PostEntityTag.class));
    }

    @Test
    void should_validate_only_newly_added_concert_when_stored_hidden_concert_tag_resent_with_new_one() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(200L))).willReturn(false);
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        verify(entityLookupService).hasHiddenConcert(Set.of(200L));
        verify(postEntityTagRepository).deleteByPostId(POST_ID);
        verify(postEntityTagRepository, times(2)).save(any(PostEntityTag.class));
    }

    @Test
    void should_not_validate_concert_tags_when_content_is_null_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", null, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        verify(postEntityTagRepository, never()).findByPostId(any());
        verify(entityLookupService, never()).hasHiddenConcert(any());
    }

    @Test
    void should_change_cover_concert_when_cover_is_in_content_cards_on_update() {
        // given: 200은 본문 카드로 새로 추가한 공개 공연
        Post post = buildPostWithCover(null);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(200L))).willReturn(false);
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, 200L);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getCoverConcertId()).isEqualTo(200L);
        verify(postEntityTagRepository).deleteByPostId(POST_ID);
    }

    @Test
    void should_change_cover_concert_when_cover_is_in_stored_tags_and_content_is_null_on_update() {
        // given
        Post post = buildPostWithCover(null);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(100L))).willReturn(false);
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, 100L);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
        verify(postEntityTagRepository, never()).deleteByPostId(any());
    }

    @Test
    void should_throw_cover_concert_not_tagged_exception_and_not_replace_tags_when_cover_not_in_content_cards_on_update() {
        // given: 200은 저장된 태그에도 본문 카드에도 없다
        Post post = buildPostWithCover(100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        PostUpdateRequest request = new PostUpdateRequest(null, null,
                contentWithCards(card(EntityType.CONCERT, 100L)), 200L);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostCoverConcertNotTaggedException.class)
                .hasMessage(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.getMessage());
        verify(postEntityTagRepository, never()).deleteByPostId(any());
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
    }

    @Test
    void should_throw_cover_concert_not_tagged_exception_when_cover_not_in_stored_tags_and_content_is_null_on_update() {
        // given
        Post post = buildPostWithCover(100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, 200L);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostCoverConcertNotTaggedException.class)
                .hasMessage(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.getMessage());
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
    }

    @Test
    void should_throw_concert_tag_hidden_exception_when_stored_hidden_concert_given_as_cover_on_update() {
        // given: 100은 저장된 태그지만 저장 이후 숨김 처리된 공연
        Post post = buildPostWithCover(null);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(100L))).willReturn(true);
        PostUpdateRequest request = new PostUpdateRequest(null, null,
                contentWithCards(card(EntityType.CONCERT, 100L)), 100L);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostConcertTagHiddenException.class)
                .hasMessage(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage());
        verify(postEntityTagRepository, never()).deleteByPostId(any());
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
        assertThat(post.getCoverConcertId()).isNull();
    }

    @Test
    void should_throw_concert_tag_hidden_exception_first_when_newly_added_hidden_concert_and_untagged_cover_given_on_update() {
        // given: 200은 본문 카드로 새로 추가한 숨김 공연, 대표 공연 300은 본문 카드에 없다
        Post post = buildPostWithCover(null);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(200L))).willReturn(true);
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.CONCERT, 200L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, 300L);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(PostConcertTagHiddenException.class)
                .hasMessage(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage());
        verify(postEntityTagRepository, never()).deleteByPostId(any());
        assertThat(post.getCoverConcertId()).isNull();
    }

    @Test
    void should_clear_cover_concert_when_cover_not_given_and_card_removed_from_content_on_update() {
        // given: 본문 수정으로 기존 대표 100의 카드를 빼고 200 카드만 남긴다
        Post post = buildPostWithCover(100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        given(entityLookupService.hasHiddenConcert(Set.of(200L))).willReturn(false);
        PostUpdateRequest request = new PostUpdateRequest(null, null,
                contentWithCards(card(EntityType.CONCERT, 200L)), null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getCoverConcertId()).isNull();
    }

    @Test
    void should_keep_cover_concert_when_cover_not_given_and_card_still_in_content_on_update() {
        // given
        Post post = buildPostWithCover(100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postEntityTagRepository.findByPostId(POST_ID)).willReturn(List.of(buildConcertTag(100L)));
        Map<String, Object> content = contentWithCards(card(EntityType.CONCERT, 100L), card(EntityType.ARTIST, 1L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
    }

    @Test
    void should_keep_cover_concert_without_querying_tags_when_cover_and_content_are_null_on_update() {
        // given
        Post post = buildPostWithCover(100L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", null, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getCoverConcertId()).isEqualTo(100L);
        verify(postEntityTagRepository, never()).findByPostId(any());
    }

    @Test
    void should_delete_then_save_entity_tags_in_card_order_when_content_given_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.REVIEW, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        Map<String, Object> content = contentWithCards(card(EntityType.ARTIST, 1L), card(EntityType.CONCERT, 100L));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        InOrder inOrder = inOrder(postEntityTagRepository);
        inOrder.verify(postEntityTagRepository).deleteByPostId(POST_ID);
        ArgumentCaptor<PostEntityTag> captor = ArgumentCaptor.forClass(PostEntityTag.class);
        inOrder.verify(postEntityTagRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PostEntityTag::getEntityType, PostEntityTag::getEntityId)
                .containsExactly(tuple(EntityType.ARTIST, 1L), tuple(EntityType.CONCERT, 100L));
    }

    @Test
    void should_update_content_without_disallowed_attrs_when_content_has_paragraph_text_align_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "attrs", Map.of("textAlign", "center"), "content", List.of(
                        Map.of("type", "text", "text", "안녕하세요")
                ))
        ));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);

        // when
        postService.update(AUTHOR_ID, POST_ID, request);

        // then
        assertThat(post.getContent()).isEqualTo(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"안녕하세요\"}]}]}");
    }

    @Test
    void should_throw_content_invalid_format_exception_and_keep_post_and_tags_when_content_has_disallowed_node_on_update() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "기존 제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", contentWithDisallowedNode(), null);

        // when & then
        assertThatThrownBy(() -> postService.update(AUTHOR_ID, POST_ID, request))
                .isInstanceOf(ContentInvalidFormatException.class)
                .hasMessage(ErrorCode.CONTENT_INVALID_FORMAT.getMessage());
        assertThat(post.getTitle()).isEqualTo("기존 제목");
        assertThat(post.getContent()).isEqualTo("{\"type\":\"doc\"}");
        verify(postEntityTagRepository, never()).deleteByPostId(any());
        verify(postEntityTagRepository, never()).save(any(PostEntityTag.class));
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

    @Test
    void should_delete_comments_comment_likes_and_recommends_when_author_deletes_post() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        // when
        postService.delete(AUTHOR_ID, POST_ID);

        // then
        verify(commentLikeRepository).deleteByCommentPostId(POST_ID);
        verify(commentRepository).deleteByPostId(POST_ID);
        verify(postRecommendRepository).deleteByPostId(POST_ID);
    }

    // -------------------------------------------------------------------------
    // adminDelete
    // -------------------------------------------------------------------------

    @Test
    void should_throw_post_not_found_exception_when_admin_deleting_post_that_does_not_exist() {
        // given
        given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.adminDelete(POST_ID))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    void should_delete_post_and_dependents_when_admin_deletes_post_authored_by_another_user() {
        // given
        Post post = buildPost(POST_ID, AUTHOR_ID, PostCategory.FREE, "제목", 0L);
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));

        // when
        postService.adminDelete(POST_ID);

        // then
        verify(commentLikeRepository).deleteByCommentPostId(POST_ID);
        verify(commentRepository).deleteByPostId(POST_ID);
        verify(postRecommendRepository).deleteByPostId(POST_ID);
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
        given(postRepository.findRecommendCountById(POST_ID)).willReturn(4L);

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
        given(postRecommendRepository.deleteByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(0L);

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
        given(postRepository.findById(POST_ID)).willReturn(Optional.of(post));
        given(postRecommendRepository.deleteByUserIdAndPostId(OTHER_USER_ID, POST_ID)).willReturn(1L);
        given(postRepository.findRecommendCountById(POST_ID)).willReturn(2L);

        // when
        RecommendCountResponse response = postService.unrecommend(OTHER_USER_ID, POST_ID);

        // then
        assertThat(response.recommendCount()).isEqualTo(2L);
        verify(postRecommendRepository).deleteByUserIdAndPostId(OTHER_USER_ID, POST_ID);
        verify(postRepository).decrementRecommendCount(POST_ID);
    }
}
