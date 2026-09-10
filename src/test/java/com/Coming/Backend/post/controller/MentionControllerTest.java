package com.Coming.Backend.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.service.MentionService;

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
class MentionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MentionService mentionService;

    @InjectMocks
    private MentionController mentionController;

    private static final Long CONCERT_ID = 1L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(mentionController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .setValidator(validator)
                .build();
    }

    @Test
    void should_return_200_with_entity_card_list_when_valid_request_given() throws Exception {
        // given
        EntityCardResponse card = new EntityCardResponse(
                EntityType.CONCERT, CONCERT_ID, "아이유 콘서트", "2025-10-01 · 올림픽공원", null);
        given(mentionService.search(eq(EntityType.CONCERT), eq("아이유"), eq(10))).willReturn(List.of(card));

        // when & then
        mockMvc.perform(get("/api/mentions/search")
                        .param("type", "CONCERT")
                        .param("q", "아이유")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("CONCERT"))
                .andExpect(jsonPath("$[0].id").value(CONCERT_ID))
                .andExpect(jsonPath("$[0].title").value("아이유 콘서트"));
    }

    @Test
    void should_return_400_when_type_is_missing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/mentions/search")
                        .param("q", "아이유")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_type_is_invalid_value() throws Exception {
        // when & then
        mockMvc.perform(get("/api/mentions/search")
                        .param("type", "INVALID_TYPE")
                        .param("q", "아이유")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_200_with_empty_array_when_no_search_result_found() throws Exception {
        // given
        given(mentionService.search(eq(EntityType.ARTIST), eq("없는아티스트"), any(Integer.class)))
                .willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/mentions/search")
                        .param("type", "ARTIST")
                        .param("q", "없는아티스트")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
