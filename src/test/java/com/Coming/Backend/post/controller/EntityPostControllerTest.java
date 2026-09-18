package com.Coming.Backend.post.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.service.PostService;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class EntityPostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @InjectMocks
    private EntityPostController entityPostController;

    private static final Long ARTIST_ID = 1L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(entityPostController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .setValidator(validator)
                .build();
    }

    private PostSummaryResponse buildSummary() {
        return new PostSummaryResponse(
                10L, "IU", PostCategory.REVIEW, "제목", List.of(), 0L, 0L, LocalDateTime.now());
    }

    @Test
    void should_return_200_with_page_response_when_default_sort_given() throws Exception {
        // given
        PageResponse<PostSummaryResponse> pageResponse =
                new PageResponse<>(List.of(buildSummary()), 0, 20, 1, 1);
        given(postService.getBacklinks(eq(EntityType.ARTIST), eq(ARTIST_ID), eq("latest"), eq(0), eq(20)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/entities/ARTIST/{id}/posts", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("제목"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_call_service_with_recommend_sort_when_sort_query_param_given() throws Exception {
        // given
        PageResponse<PostSummaryResponse> pageResponse =
                new PageResponse<>(List.of(buildSummary()), 0, 20, 1, 1);
        given(postService.getBacklinks(eq(EntityType.ARTIST), eq(ARTIST_ID), eq("recommend"), eq(0), eq(20)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/entities/ARTIST/{id}/posts", ARTIST_ID)
                        .param("sort", "recommend")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_service_throws_invalid_input_exception() throws Exception {
        // given
        given(postService.getBacklinks(eq(EntityType.ARTIST), eq(ARTIST_ID), eq("oldest"), eq(0), eq(20)))
                .willThrow(new InvalidInputException());

        // when & then
        mockMvc.perform(get("/api/entities/ARTIST/{id}/posts", ARTIST_ID)
                        .param("sort", "oldest")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_type_path_variable_is_invalid_entity_type() throws Exception {
        // when & then
        mockMvc.perform(get("/api/entities/INVALID/{id}/posts", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
