package com.Coming.Backend.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.calendar.service.CalendarService;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryExistsResponse;
import com.Coming.Backend.inquiry.dto.InquiryListItemResponse;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.Coming.Backend.inquiry.service.InquiryService;
import com.Coming.Backend.user.dto.ConcertHistoryResponse;
import com.Coming.Backend.user.service.UserService;
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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CalendarService calendarService;

    @Mock
    private UserService userService;

    @Mock
    private InquiryService inquiryService;

    @InjectMocks
    private UserController userController;

    private static final Long CONCERT_ID = 1L;
    private static final Long INQUIRY_ID = 2L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/me/concerts/upcoming
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_upcoming_concerts() throws Exception {
        // given
        CalendarEntryResponse entry = new CalendarEntryResponse(
                CONCERT_ID, "CONCERT", List.of(), "YOASOBI LIVE",
                LocalDate.of(2025, 8, 15), LocalDate.of(2025, 8, 15),
                "UPCOMING", "poster.jpg", "올림픽홀", true, null
        );
        PageResponse<CalendarEntryResponse> pageResponse = new PageResponse<>(List.of(entry), 0, 10, 1, 1);
        given(calendarService.getMyCalendar(isNull(), any(Pageable.class))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/me/concerts/upcoming").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].concertId").value(CONCERT_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // GET /api/me/concerts/history
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_concert_history() throws Exception {
        // given
        ConcertHistoryResponse history = new ConcertHistoryResponse(
                CONCERT_ID, List.of(), "YOASOBI LIVE",
                LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 1),
                "올림픽홀", "ENDED"
        );
        PageResponse<ConcertHistoryResponse> pageResponse = new PageResponse<>(List.of(history), 0, 10, 1, 1);
        given(userService.getHistory(isNull(), any(Pageable.class))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/me/concerts/history").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(CONCERT_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // GET /api/me/inquiries
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_all_inquiries_when_no_status_filter() throws Exception {
        // given
        InquiryListItemResponse item = new InquiryListItemResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.PENDING,
                "2024-06-01", null, null
        );
        PageResponse<InquiryListItemResponse> pageResponse = new PageResponse<>(List.of(item), 0, 10, 1, 1);
        given(inquiryService.getMyInquiries(isNull(), isNull(), any(Pageable.class))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/me/inquiries").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(INQUIRY_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_filtered_inquiries_when_status_given() throws Exception {
        // given
        InquiryListItemResponse item = new InquiryListItemResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.RESOLVED,
                "2024-06-01", "처리 완료되었습니다.", null
        );
        PageResponse<InquiryListItemResponse> pageResponse = new PageResponse<>(List.of(item), 0, 10, 1, 1);
        given(inquiryService.getMyInquiries(isNull(), eq(InquiryStatus.RESOLVED), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/me/inquiries")
                        .param("status", "RESOLVED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[0].resultMessage").value("처리 완료되었습니다."));
    }

    // -------------------------------------------------------------------------
    // GET /api/me/inquiries/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_inquiry_detail() throws Exception {
        // given
        InquiryDetailResponse detail = new InquiryDetailResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.PENDING,
                "2024-06-01", null, null, "공연 날짜가 잘못 표기되어 있습니다."
        );
        given(inquiryService.getMyInquiryDetail(isNull(), eq(INQUIRY_ID))).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/me/inquiries/{id}", INQUIRY_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INQUIRY_ID))
                .andExpect(jsonPath("$.type").value("CONCERT"))
                .andExpect(jsonPath("$.content").value("공연 날짜가 잘못 표기되어 있습니다."));
    }

    @Test
    void should_return_404_when_inquiry_not_found() throws Exception {
        // given
        given(inquiryService.getMyInquiryDetail(isNull(), eq(999L)))
                .willThrow(new InquiryNotFoundException());

        // when & then
        mockMvc.perform(get("/api/me/inquiries/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.INQUIRY_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // GET /api/me/inquiries/exists
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_exists_true_when_pending_inquiry_found() throws Exception {
        // given
        given(inquiryService.existsPendingInquiry(isNull(), eq(InquiryType.CONCERT), eq(1L)))
                .willReturn(new InquiryExistsResponse(true));

        // when & then
        mockMvc.perform(get("/api/me/inquiries/exists")
                        .param("type", "CONCERT")
                        .param("targetId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void should_return_200_with_exists_false_when_no_pending_inquiry() throws Exception {
        // given
        given(inquiryService.existsPendingInquiry(isNull(), eq(InquiryType.ARTIST), eq(2L)))
                .willReturn(new InquiryExistsResponse(false));

        // when & then
        mockMvc.perform(get("/api/me/inquiries/exists")
                        .param("type", "ARTIST")
                        .param("targetId", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }
}
