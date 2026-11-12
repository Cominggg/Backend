package com.Coming.Backend.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ContentInvalidFormatException;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.PostCreateRequest;
import com.Coming.Backend.post.dto.PostCreateResponse;
import com.Coming.Backend.post.dto.PostDetailResponse;
import com.Coming.Backend.post.dto.PostEntityTagResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.dto.PostUpdateRequest;
import com.Coming.Backend.post.dto.RecommendCountResponse;
import com.Coming.Backend.post.dto.TrendingTagResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.exception.AlreadyRecommendedException;
import com.Coming.Backend.post.exception.NotRecommendedException;
import com.Coming.Backend.post.exception.PostConcertTagHiddenException;
import com.Coming.Backend.post.exception.PostCoverConcertNotTaggedException;
import com.Coming.Backend.post.exception.PostForbiddenException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long CONCERT_ID = 300L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new GlobalExceptionHandler(new com.Coming.Backend.common.discord.NoOpDiscordNotifier()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Map<String, Object> sampleContent() {
        return Map.of("type", "doc", "content", List.of());
    }

    private Map<String, Object> contentWithConcertCard() {
        return Map.of("type", "doc", "content", List.of(
                Map.of("type", "entityMentionCard", "attrs", Map.of(
                        "entityType", "CONCERT", "entityId", CONCERT_ID, "title", "HER"))
        ));
    }

    private Map<String, Object> contentWithArtistCards(int count) {
        return Map.of("type", "doc", "content", IntStream.rangeClosed(1, count)
                .mapToObj(i -> Map.of("type", "entityMentionCard", "attrs", Map.of(
                        "entityType", "ARTIST", "entityId", (long) i, "title", "아티스트 " + i)))
                .toList());
    }

    private PostDetailResponse detailWithCoverConcertId(Long coverConcertId) {
        return new PostDetailResponse(
                POST_ID, "IU", PostCategory.REVIEW, "콘서트 후기", sampleContent(),
                List.of(new PostEntityTagResponse(EntityType.CONCERT, CONCERT_ID, "HER", "KSPO DOME", null, null)),
                coverConcertId, 0L, 1L, 0L, null, true, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // -------------------------------------------------------------------------
    // POST /api/posts
    // -------------------------------------------------------------------------

    @Test
    void should_return_201_when_create_request_is_valid() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", sampleContent(), null);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class))).willReturn(new PostCreateResponse(POST_ID));

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(POST_ID));
    }

    @Test
    void should_return_400_when_service_throws_invalid_input_exception_on_create() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.REVIEW, "리뷰 제목", sampleContent(), null);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class))).willThrow(new InvalidInputException());

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_pass_cover_concert_id_to_service_when_create_request_has_cover_concert_id() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(
                PostCategory.REVIEW, "콘서트 후기", contentWithConcertCard(), CONCERT_ID);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class))).willReturn(new PostCreateResponse(POST_ID));
        ArgumentCaptor<PostCreateRequest> captor = ArgumentCaptor.forClass(PostCreateRequest.class);

        // when
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // then
        verify(postService).create(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().coverConcertId()).isEqualTo(CONCERT_ID);
    }

    @Test
    void should_return_400_when_service_throws_concert_tag_hidden_exception_on_create() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(
                PostCategory.REVIEW, "콘서트 후기", contentWithConcertCard(), null);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class)))
                .willThrow(new PostConcertTagHiddenException());

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_CONCERT_TAG_HIDDEN.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage()));
    }

    @Test
    void should_return_400_when_service_throws_cover_concert_not_tagged_exception_on_create() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(
                PostCategory.REVIEW, "콘서트 후기", sampleContent(), CONCERT_ID);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class)))
                .willThrow(new PostCoverConcertNotTaggedException());

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.getMessage()));
    }

    @Test
    void should_return_400_when_service_throws_content_invalid_format_exception_for_disallowed_node_on_create() throws Exception {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "codeBlock", "content", List.of(Map.of("type", "text", "text", "System.out.println();")))
        ));
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", content, null);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class)))
                .willThrow(new ContentInvalidFormatException());

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_INVALID_FORMAT"))
                .andExpect(jsonPath("$.message").value("허용되지 않는 본문 형식입니다."));
    }

    @Test
    void should_return_400_when_service_throws_content_invalid_format_exception_for_disallowed_mark_on_create() throws Exception {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "content", List.of(
                        Map.of("type", "text", "text", "기울임", "marks", List.of(Map.of("type", "italic")))
                ))
        ));
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", content, null);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class)))
                .willThrow(new ContentInvalidFormatException());

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_INVALID_FORMAT"))
                .andExpect(jsonPath("$.message").value("허용되지 않는 본문 형식입니다."));
    }

    @Test
    void should_return_400_when_service_throws_content_invalid_format_exception_for_11_unique_cards_on_create() throws Exception {
        // given
        PostCreateRequest request = new PostCreateRequest(PostCategory.FREE, "자유 제목", contentWithArtistCards(11), null);
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class)))
                .willThrow(new ContentInvalidFormatException());

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_INVALID_FORMAT"))
                .andExpect(jsonPath("$.message").value("허용되지 않는 본문 형식입니다."));
    }

    @Test
    void should_return_201_and_ignore_legacy_entity_tags_field_when_create_request_has_entity_tags() throws Exception {
        // given: 구버전 클라이언트가 보내던 entityTags 필드를 그대로 포함한 요청
        String requestJson = """
                {"category":"FREE","title":"자유 제목","content":{"type":"doc","content":[]},
                 "entityTags":[{"entityType":"ARTIST","entityId":1}],"coverConcertId":null}
                """;
        given(postService.create(eq(USER_ID), any(PostCreateRequest.class))).willReturn(new PostCreateResponse(POST_ID));
        ArgumentCaptor<PostCreateRequest> captor = ArgumentCaptor.forClass(PostCreateRequest.class);

        // when
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(POST_ID));

        // then
        verify(postService).create(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("자유 제목");
        assertThat(captor.getValue().content()).isEqualTo(Map.of("type", "doc", "content", List.of()));
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_post_detail_when_post_exists() throws Exception {
        // given
        PostDetailResponse detail = new PostDetailResponse(
                POST_ID, "IU", PostCategory.FREE, "제목", sampleContent(),
                List.of(), null, 0L, 1L, 0L, null, true, LocalDateTime.now(), LocalDateTime.now()
        );
        given(postService.getDetail(eq(POST_ID), eq(USER_ID))).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/posts/{id}", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(POST_ID))
                .andExpect(jsonPath("$.title").value("제목"));
    }

    @Test
    void should_return_cover_concert_id_when_post_detail_has_cover_concert() throws Exception {
        // given
        given(postService.getDetail(eq(POST_ID), eq(USER_ID))).willReturn(detailWithCoverConcertId(CONCERT_ID));

        // when & then
        mockMvc.perform(get("/api/posts/{id}", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverConcertId").value(CONCERT_ID))
                .andExpect(jsonPath("$.entityTags[0].entityId").value(CONCERT_ID));
    }

    @Test
    void should_serialize_cover_concert_id_as_null_when_post_detail_has_no_cover_concert() throws Exception {
        // given
        given(postService.getDetail(eq(POST_ID), eq(USER_ID))).willReturn(detailWithCoverConcertId(null));

        // when & then
        mockMvc.perform(get("/api/posts/{id}", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverConcertId").hasJsonPath())
                .andExpect(jsonPath("$.coverConcertId").value(nullValue()));
    }

    @Test
    void should_serialize_entity_tags_in_service_order_when_post_detail_has_multiple_tags() throws Exception {
        // given
        PostDetailResponse detail = new PostDetailResponse(
                POST_ID, "IU", PostCategory.REVIEW, "콘서트 후기", sampleContent(),
                List.of(
                        new PostEntityTagResponse(EntityType.CONCERT, 200L, "The Golden Hour", "잠실 올림픽주경기장", null, null),
                        new PostEntityTagResponse(EntityType.ARTIST, 1L, "IU", null, null, null),
                        new PostEntityTagResponse(EntityType.CONCERT, 100L, "HER", "KSPO DOME", null, null)
                ),
                null, 0L, 1L, 0L, null, true, LocalDateTime.now(), LocalDateTime.now()
        );
        given(postService.getDetail(eq(POST_ID), eq(USER_ID))).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/posts/{id}", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityTags.length()").value(3))
                .andExpect(jsonPath("$.entityTags[0].entityType").value("CONCERT"))
                .andExpect(jsonPath("$.entityTags[0].entityId").value(200L))
                .andExpect(jsonPath("$.entityTags[1].entityType").value("ARTIST"))
                .andExpect(jsonPath("$.entityTags[1].entityId").value(1L))
                .andExpect(jsonPath("$.entityTags[2].entityType").value("CONCERT"))
                .andExpect(jsonPath("$.entityTags[2].entityId").value(100L));
    }

    @Test
    void should_return_404_when_post_not_found_on_get_detail() throws Exception {
        // given
        given(postService.getDetail(eq(999L), eq(USER_ID))).willThrow(new PostNotFoundException());

        // when & then
        mockMvc.perform(get("/api/posts/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // GET /api/posts
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_page_response_when_getting_list() throws Exception {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(
                POST_ID, "IU", PostCategory.FREE, "제목", List.of(), null, 0L, 0L, LocalDateTime.now());
        PageResponse<PostSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 20, 1, 1);
        given(postService.getList(isNull(), eq(0), eq(20))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/posts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(POST_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_cover_concert_id_in_list_item_when_post_has_cover_concert() throws Exception {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(
                POST_ID, "IU", PostCategory.REVIEW, "콘서트 후기",
                List.of(new PostEntityTagResponse(EntityType.CONCERT, CONCERT_ID, "HER", "KSPO DOME", null, null)),
                CONCERT_ID, 0L, 0L, LocalDateTime.now());
        PageResponse<PostSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 20, 1, 1);
        given(postService.getList(isNull(), eq(0), eq(20))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/posts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].coverConcertId").value(CONCERT_ID));
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/popular
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_popular_posts_when_default_params_given() throws Exception {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(
                POST_ID, "IU", PostCategory.FREE, "인기 게시글", List.of(), null, 10L, 0L, LocalDateTime.now());
        given(postService.getPopular(eq(7), eq(5))).willReturn(List.of(summary));

        // when & then
        mockMvc.perform(get("/api/posts/popular").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(POST_ID))
                .andExpect(jsonPath("$[0].title").value("인기 게시글"));
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/popular-board
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_page_response_when_getting_popular_board_with_default_params() throws Exception {
        // given
        PostSummaryResponse summary = new PostSummaryResponse(
                POST_ID, "IU", PostCategory.FREE, "인기글", List.of(), null, 10L, 0L, LocalDateTime.now());
        PageResponse<PostSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 20, 1, 1);
        given(postService.getPopularBoard(eq(0), eq(20))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/posts/popular-board").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(POST_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/trending-tags
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_trending_tags_when_default_params_given() throws Exception {
        // given
        TrendingTagResponse tag = new TrendingTagResponse(EntityType.ARTIST, 1L, "IU", 5L);
        given(postService.getTrendingTags(eq(7), eq(10))).willReturn(List.of(tag));

        // when & then
        mockMvc.perform(get("/api/posts/trending-tags").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("ARTIST"))
                .andExpect(jsonPath("$[0].entityId").value(1))
                .andExpect(jsonPath("$[0].title").value("IU"))
                .andExpect(jsonPath("$[0].count").value(5));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/posts/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_update_request_is_valid() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", null, null);
        willDoNothing().given(postService).update(eq(USER_ID), eq(POST_ID), any(PostUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_403_when_service_throws_forbidden_exception_on_update() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(null, "새 제목", null, null);
        willThrow(new PostForbiddenException()).given(postService)
                .update(eq(USER_ID), eq(POST_ID), any(PostUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_update_request_has_blank_title() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(null, "   ", null, null);

        // when & then
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
    }

    @Test
    void should_pass_cover_concert_id_to_service_when_update_request_has_cover_concert_id() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(null, null, contentWithConcertCard(), CONCERT_ID);
        willDoNothing().given(postService).update(eq(USER_ID), eq(POST_ID), any(PostUpdateRequest.class));
        ArgumentCaptor<PostUpdateRequest> captor = ArgumentCaptor.forClass(PostUpdateRequest.class);

        // when
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        verify(postService).update(eq(USER_ID), eq(POST_ID), captor.capture());
        assertThat(captor.getValue().coverConcertId()).isEqualTo(CONCERT_ID);
    }

    @Test
    void should_return_400_when_service_throws_concert_tag_hidden_exception_on_update() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(null, null, contentWithConcertCard(), null);
        willThrow(new PostConcertTagHiddenException()).given(postService)
                .update(eq(USER_ID), eq(POST_ID), any(PostUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_CONCERT_TAG_HIDDEN.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.POST_CONCERT_TAG_HIDDEN.getMessage()));
    }

    @Test
    void should_return_400_when_service_throws_cover_concert_not_tagged_exception_on_update() throws Exception {
        // given
        PostUpdateRequest request = new PostUpdateRequest(null, null, null, CONCERT_ID);
        willThrow(new PostCoverConcertNotTaggedException()).given(postService)
                .update(eq(USER_ID), eq(POST_ID), any(PostUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED.getMessage()));
    }

    @Test
    void should_return_400_when_service_throws_content_invalid_format_exception_for_disallowed_heading_level_on_update() throws Exception {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "heading", "attrs", Map.of("level", 1), "content", List.of(
                        Map.of("type", "text", "text", "큰 제목")
                ))
        ));
        PostUpdateRequest request = new PostUpdateRequest(null, null, content, null);
        willThrow(new ContentInvalidFormatException()).given(postService)
                .update(eq(USER_ID), eq(POST_ID), any(PostUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/posts/{id}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_INVALID_FORMAT"))
                .andExpect(jsonPath("$.message").value("허용되지 않는 본문 형식입니다."));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/posts/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_delete_succeeds() throws Exception {
        // given
        willDoNothing().given(postService).delete(eq(USER_ID), eq(POST_ID));

        // when & then
        mockMvc.perform(delete("/api/posts/{id}", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_404_when_post_not_found_on_delete() throws Exception {
        // given
        willThrow(new PostNotFoundException()).given(postService).delete(eq(USER_ID), eq(999L));

        // when & then
        mockMvc.perform(delete("/api/posts/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // POST /api/posts/{id}/recommend
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_recommend_count_when_recommend_succeeds() throws Exception {
        // given
        given(postService.recommend(eq(USER_ID), eq(POST_ID))).willReturn(new RecommendCountResponse(4L));

        // when & then
        mockMvc.perform(post("/api/posts/{id}/recommend", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendCount").value(4));
    }

    @Test
    void should_return_404_when_post_not_found_on_recommend() throws Exception {
        // given
        given(postService.recommend(eq(USER_ID), eq(999L))).willThrow(new PostNotFoundException());

        // when & then
        mockMvc.perform(post("/api/posts/{id}/recommend", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_409_when_post_already_recommended_on_recommend() throws Exception {
        // given
        given(postService.recommend(eq(USER_ID), eq(POST_ID))).willThrow(new AlreadyRecommendedException());

        // when & then
        mockMvc.perform(post("/api/posts/{id}/recommend", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ALREADY_RECOMMENDED.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/posts/{id}/recommend
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_recommend_count_when_unrecommend_succeeds() throws Exception {
        // given
        given(postService.unrecommend(eq(USER_ID), eq(POST_ID))).willReturn(new RecommendCountResponse(2L));

        // when & then
        mockMvc.perform(delete("/api/posts/{id}/recommend", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendCount").value(2));
    }

    @Test
    void should_return_404_when_post_not_found_on_unrecommend() throws Exception {
        // given
        given(postService.unrecommend(eq(USER_ID), eq(999L))).willThrow(new PostNotFoundException());

        // when & then
        mockMvc.perform(delete("/api/posts/{id}/recommend", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_post_not_recommended_on_unrecommend() throws Exception {
        // given
        given(postService.unrecommend(eq(USER_ID), eq(POST_ID))).willThrow(new NotRecommendedException());

        // when & then
        mockMvc.perform(delete("/api/posts/{id}/recommend", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_RECOMMENDED.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
