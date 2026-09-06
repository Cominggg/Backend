package com.Coming.Backend.concert.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.dto.ArtistSummary;
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
                .setControllerAdvice(new GlobalExceptionHandler(new com.Coming.Backend.common.discord.NoOpDiscordNotifier()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    private ConcertSummaryResponse buildSummary(Long id, String title, String artistName) {
        List<ArtistSummary> artists = artistName != null
                ? List.of(new ArtistSummary(1L, artistName, null))
                : List.of();
        return new ConcertSummaryResponse(
                id,
                "https://example.com/poster.jpg",
                artists,
                title,
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 3),
                "올림픽공원",
                ConcertStatus.UPCOMING,
                false,
                null
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/concerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_concerts_when_no_query_params_given() throws Exception {
        // given
        ConcertSummaryResponse summary = buildSummary(CONCERT_ID, "IU CONCERT", "IU");
        PageResponse<ConcertSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 20, 1, 1);
        given(concertService.getConcerts(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(CONCERT_ID))
                .andExpect(jsonPath("$.content[0].title").value("IU CONCERT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(concertService).getConcerts(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class), isNull());
    }

    @Test
    void should_pass_all_query_params_to_service_when_all_filters_given() throws Exception {
        // given
        PageResponse<ConcertSummaryResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0);
        given(concertService.getConcerts(eq("IU"), eq(ConcertStatus.UPCOMING), eq(true), eq(true), eq(true), any(Pageable.class), isNull()))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .param("q", "IU")
                        .param("status", "UPCOMING")
                        .param("inCalendar", "true")
                        .param("followedOnly", "true")
                        .param("ticketOpenPending", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(concertService).getConcerts(eq("IU"), eq(ConcertStatus.UPCOMING), eq(true), eq(true), eq(true), any(Pageable.class), isNull());
    }

    @Test
    void should_return_200_with_empty_page_when_no_concerts_match_filters() throws Exception {
        // given
        PageResponse<ConcertSummaryResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0);
        given(concertService.getConcerts(eq("없는공연"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .param("q", "없는공연")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void should_return_400_when_sort_property_is_not_allowed() throws Exception {
        // given
        // sort로 허용되지 않은 필드(예: id)가 오면 SortPropertyValidator가 INVALID_INPUT을 던진다

        // when & then
        mockMvc.perform(get("/api/concerts")
                        .param("sort", "id,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
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

    // -------------------------------------------------------------------------
    // GET /api/concerts/ticketing
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_ticketing_concerts_when_no_following_param() throws Exception {
        // given
        ConcertSummaryResponse summary = buildSummary(CONCERT_ID, "IU CONCERT", "IU");
        given(concertService.getTicketingConcerts(isNull(), eq(false))).willReturn(List.of(summary));

        // when & then
        mockMvc.perform(get("/api/concerts/ticketing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONCERT_ID));
    }

    @Test
    void should_return_401_with_error_body_when_following_true_and_user_not_authenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/api/concerts/ticketing")
                        .param("following", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
