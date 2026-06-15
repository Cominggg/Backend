package com.Coming.Backend.concert.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.dto.ConcertSummaryResponse;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.service.ConcertService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class ConcertControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConcertService concertService;

    @InjectMocks
    private ConcertController concertController;

    private static final Long CONCERT_ID = 1L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(concertController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    private ConcertSummaryResponse buildSummary(Long id, String title, String artistName) {
        return new ConcertSummaryResponse(
                id,
                "https://example.com/poster.jpg",
                artistName,
                title,
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 3),
                "올림픽공원",
                ConcertStatus.UPCOMING,
                false
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/concerts/search
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_search_results_when_query_matches_concerts() throws Exception {
        // given
        ConcertSummaryResponse summary = buildSummary(CONCERT_ID, "IU CONCERT", "IU");
        PageResponse<ConcertSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 20, 1, 1);
        given(concertService.searchConcerts(eq("IU"), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/concerts/search")
                        .param("q", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(CONCERT_ID))
                .andExpect(jsonPath("$.content[0].title").value("IU CONCERT"))
                .andExpect(jsonPath("$.content[0].artistName").value("IU"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_empty_result_when_query_matches_no_concerts() throws Exception {
        // given
        PageResponse<ConcertSummaryResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0);
        given(concertService.searchConcerts(eq("없는공연"), any(Pageable.class), isNull()))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/concerts/search")
                        .param("q", "없는공연")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void should_return_200_with_multiple_results_when_query_is_partial_match() throws Exception {
        // given
        ConcertSummaryResponse result1 = buildSummary(1L, "IU CONCERT 2025", "IU");
        ConcertSummaryResponse result2 = buildSummary(2L, "IU 앙코르 공연", "IU");
        PageResponse<ConcertSummaryResponse> pageResponse = new PageResponse<>(List.of(result1, result2), 0, 20, 2, 1);
        given(concertService.searchConcerts(eq("IU"), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/concerts/search")
                        .param("q", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    // -------------------------------------------------------------------------
    // GET /api/concerts?inCalendar=true
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_calendar_concerts_when_in_calendar_is_true_and_user_authenticated() throws Exception {
        // given
        ConcertSummaryResponse summary = buildSummary(CONCERT_ID, "캘린더 공연", "IU");
        PageResponse<ConcertSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 20, 1, 1);
        given(concertService.getConcerts(isNull(), eq(true), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .param("inCalendar", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("캘린더 공연"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_empty_page_when_in_calendar_is_true_and_user_is_not_authenticated() throws Exception {
        // given
        PageResponse<ConcertSummaryResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0);
        given(concertService.getConcerts(isNull(), eq(true), any(Pageable.class), isNull()))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .param("inCalendar", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void should_return_400_when_q_is_missing() throws Exception {
        // given
        // q 파라미터 자체가 없으면 MissingServletRequestParameterException → 400

        // when & then
        mockMvc.perform(get("/api/concerts/search")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
    }

    // -------------------------------------------------------------------------
    // 오류 응답 형식 검증
    // -------------------------------------------------------------------------

    @Test
    void should_return_404_with_error_body_when_concert_not_found() throws Exception {
        // given
        given(concertService.getConcert(eq(999L), isNull())).willThrow(new ConcertNotFoundException());

        // when & then
        mockMvc.perform(get("/api/concerts/{id}", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
