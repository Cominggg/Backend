package com.Coming.Backend.post.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.post.dto.CommentLikeCountResponse;
import com.Coming.Backend.post.exception.AlreadyLikedException;
import com.Coming.Backend.post.exception.CommentForbiddenException;
import com.Coming.Backend.post.exception.CommentNotFoundException;
import com.Coming.Backend.post.exception.NotLikedException;
import com.Coming.Backend.post.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long COMMENT_ID = 100L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
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

    // -------------------------------------------------------------------------
    // DELETE /api/comments/{commentId}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_delete_succeeds() throws Exception {
        // given
        willDoNothing().given(commentService).delete(eq(USER_ID), eq(COMMENT_ID));

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_404_when_comment_not_found_on_delete() throws Exception {
        // given
        willThrow(new CommentNotFoundException()).given(commentService).delete(eq(USER_ID), eq(999L));

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_403_when_not_author_on_delete() throws Exception {
        // given
        willThrow(new CommentForbiddenException()).given(commentService).delete(eq(USER_ID), eq(COMMENT_ID));

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // POST /api/comments/{commentId}/like
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_like_count_when_like_succeeds() throws Exception {
        // given
        given(commentService.like(eq(USER_ID), eq(COMMENT_ID))).willReturn(new CommentLikeCountResponse(5L));

        // when & then
        mockMvc.perform(post("/api/comments/{commentId}/like", COMMENT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(5));
    }

    @Test
    void should_return_404_when_comment_not_found_on_like() throws Exception {
        // given
        given(commentService.like(eq(USER_ID), eq(999L))).willThrow(new CommentNotFoundException());

        // when & then
        mockMvc.perform(post("/api/comments/{commentId}/like", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_409_when_already_liked_on_like() throws Exception {
        // given
        given(commentService.like(eq(USER_ID), eq(COMMENT_ID))).willThrow(new AlreadyLikedException());

        // when & then
        mockMvc.perform(post("/api/comments/{commentId}/like", COMMENT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ALREADY_LIKED.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/comments/{commentId}/like
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_like_count_when_unlike_succeeds() throws Exception {
        // given
        given(commentService.unlike(eq(USER_ID), eq(COMMENT_ID))).willReturn(new CommentLikeCountResponse(3L));

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}/like", COMMENT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(3));
    }

    @Test
    void should_return_404_when_comment_not_found_on_unlike() throws Exception {
        // given
        given(commentService.unlike(eq(USER_ID), eq(999L))).willThrow(new CommentNotFoundException());

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}/like", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_not_liked_on_unlike() throws Exception {
        // given
        given(commentService.unlike(eq(USER_ID), eq(COMMENT_ID))).willThrow(new NotLikedException());

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}/like", COMMENT_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_LIKED.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
