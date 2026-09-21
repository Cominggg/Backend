package com.Coming.Backend.notice.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.notice.dto.NoticeDetailResponse;
import com.Coming.Backend.notice.dto.NoticeSummaryResponse;
import com.Coming.Backend.notice.exception.NoticeNotFoundException;
import com.Coming.Backend.notice.service.NoticeService;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class NoticeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private NoticeController noticeController;

    private static final Long NOTICE_ID = 10L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(noticeController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/notices
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_notice_list_when_default_params_given() throws Exception {
        // given
        NoticeSummaryResponse summary = new NoticeSummaryResponse(NOTICE_ID, "점검 안내", LocalDateTime.now());
        given(noticeService.getRecent(eq(5))).willReturn(List.of(summary));

        // when & then
        mockMvc.perform(get("/api/notices").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(NOTICE_ID))
                .andExpect(jsonPath("$[0].title").value("점검 안내"));
    }

    // -------------------------------------------------------------------------
    // GET /api/notices/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_notice_detail_when_notice_exists() throws Exception {
        // given
        NoticeDetailResponse detail = new NoticeDetailResponse(
                NOTICE_ID, "점검 안내", "점검은 새벽 2시부터 진행됩니다", LocalDateTime.now(), LocalDateTime.now());
        given(noticeService.getDetail(eq(NOTICE_ID))).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/notices/{id}", NOTICE_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTICE_ID))
                .andExpect(jsonPath("$.title").value("점검 안내"));
    }

    @Test
    void should_return_404_when_notice_not_found_on_get_detail() throws Exception {
        // given
        given(noticeService.getDetail(eq(999L))).willThrow(new NoticeNotFoundException());

        // when & then
        mockMvc.perform(get("/api/notices/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOTICE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
