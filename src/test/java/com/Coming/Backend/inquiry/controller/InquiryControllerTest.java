package com.Coming.Backend.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryListItemResponse;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryAlreadyPendingException;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.Coming.Backend.inquiry.exception.TargetNotFoundException;
import com.Coming.Backend.inquiry.service.InquiryService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class InquiryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InquiryService inquiryService;

    @InjectMocks
    private InquiryController inquiryController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long INQUIRY_ID = 1L;
    private static final Long TARGET_ID = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inquiryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/inquiries
    // -------------------------------------------------------------------------

    @Test
    void should_return_201_when_inquiry_created_successfully() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.CONCERT, TARGET_ID, "공연 정보 오류", "공연 날짜가 잘못 표기되어 있습니다."
        );
        willDoNothing().given(inquiryService).createInquiry(isNull(), any(InquiryCreateRequest.class));

        // when & then
        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void should_return_409_when_inquiry_already_pending() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.ARTIST, TARGET_ID, "아티스트 정보 오류", "아티스트 이름이 잘못 표기되어 있습니다."
        );
        willThrow(new InquiryAlreadyPendingException())
                .given(inquiryService).createInquiry(isNull(), any(InquiryCreateRequest.class));

        // when & then
        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.INQUIRY_ALREADY_PENDING.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_404_when_target_not_found() throws Exception {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.SETLIST, 999L, "셋리스트 오류", "셋리스트 곡 순서가 다릅니다."
        );
        willThrow(new TargetNotFoundException())
                .given(inquiryService).createInquiry(isNull(), any(InquiryCreateRequest.class));

        // when & then
        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TARGET_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_required_field_is_missing() throws Exception {
        // given — type 필드 누락
        String requestBody = """
                {
                    "targetId": 10,
                    "title": "공연 정보 오류",
                    "content": "공연 날짜가 잘못 표기되어 있습니다."
                }
                """;

        // when & then
        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/inquiries/my
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_all_inquiries_when_no_status_filter() throws Exception {
        // given
        InquiryListItemResponse item = new InquiryListItemResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.PENDING,
                "2024-06-01", null, null
        );
        PageResponse<InquiryListItemResponse> pageResponse = new PageResponse<>(List.of(item), 0, 10, 1, 1);
        given(inquiryService.getMyInquiries(isNull(), isNull(), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/inquiries/my").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(INQUIRY_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
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
        mockMvc.perform(get("/api/inquiries/my")
                        .param("status", "RESOLVED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[0].resultMessage").value("처리 완료되었습니다."));
    }

    // -------------------------------------------------------------------------
    // GET /api/inquiries/my/{id}
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
        mockMvc.perform(get("/api/inquiries/my/{id}", INQUIRY_ID).accept(MediaType.APPLICATION_JSON))
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
        mockMvc.perform(get("/api/inquiries/my/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.INQUIRY_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
