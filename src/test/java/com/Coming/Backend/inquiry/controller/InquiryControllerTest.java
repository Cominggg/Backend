package com.Coming.Backend.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryAlreadyPendingException;
import com.Coming.Backend.inquiry.exception.TargetNotFoundException;
import com.Coming.Backend.inquiry.service.InquiryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private static final Long TARGET_ID = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inquiryController)
                .setControllerAdvice(new GlobalExceptionHandler(new com.Coming.Backend.common.discord.NoOpDiscordNotifier()))
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
}
