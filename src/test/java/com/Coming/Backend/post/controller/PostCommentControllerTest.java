package com.Coming.Backend.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.CommentCreateRequest;
import com.Coming.Backend.post.dto.CommentCreateResponse;
import com.Coming.Backend.post.dto.CommentResponse;
import com.Coming.Backend.post.exception.CommentNotFoundException;
import com.Coming.Backend.post.exception.InvalidReplyDepthException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PostCommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private PostCommentController postCommentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long PARENT_COMMENT_ID = 100L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(postCommentController)
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

    private CommentResponse sampleComment() {
        return new CommentResponse(PARENT_COMMENT_ID, "IU", true, "좋은 게시글이네요", 0L, false,
                LocalDateTime.now(), List.of());
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/{postId}/comments
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_comments_when_authenticated_user_requests() throws Exception {
        // given
        PageResponse<CommentResponse> pageResponse = new PageResponse<>(List.of(sampleComment()), 0, 20, 1, 1);
        given(commentService.getComments(eq(POST_ID), eq(USER_ID), eq(0), eq(20))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(PARENT_COMMENT_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_comments_when_anonymous_user_requests() throws Exception {
        // given
        SecurityContextHolder.getContext().setAuthentication(null);
        PageResponse<CommentResponse> pageResponse = new PageResponse<>(List.of(sampleComment()), 0, 20, 1, 1);
        given(commentService.getComments(eq(POST_ID), isNull(), eq(0), eq(20))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", POST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_404_when_post_not_found_on_get_comments() throws Exception {
        // given
        given(commentService.getComments(eq(999L), eq(USER_ID), eq(0), eq(20))).willThrow(new PostNotFoundException());

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // POST /api/posts/{postId}/comments
    // -------------------------------------------------------------------------

    @Test
    void should_return_201_when_create_request_is_valid() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest("좋은 글이네요", null);
        given(commentService.create(eq(USER_ID), eq(POST_ID), any(CommentCreateRequest.class)))
                .willReturn(new CommentCreateResponse(PARENT_COMMENT_ID));

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PARENT_COMMENT_ID));
    }

    @Test
    void should_return_400_when_content_is_blank_on_create() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest("", null);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_404_when_post_not_found_on_create() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest("좋은 글이네요", null);
        given(commentService.create(eq(USER_ID), eq(999L), any(CommentCreateRequest.class)))
                .willThrow(new PostNotFoundException());

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_404_when_parent_comment_not_found_on_create() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest("답글입니다", 999L);
        given(commentService.create(eq(USER_ID), eq(POST_ID), any(CommentCreateRequest.class)))
                .willThrow(new CommentNotFoundException());

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_reply_to_reply_on_create() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest("답글의 답글입니다", PARENT_COMMENT_ID);
        given(commentService.create(eq(USER_ID), eq(POST_ID), any(CommentCreateRequest.class)))
                .willThrow(new InvalidReplyDepthException());

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REPLY_DEPTH.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
