package com.Coming.Backend.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.discord.DiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryExistsResponse;
import com.Coming.Backend.inquiry.dto.InquiryListItemResponse;
import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryAlreadyPendingException;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.Coming.Backend.inquiry.exception.TargetNotFoundException;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @InjectMocks
    private InquiryService inquiryService;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private DiscordNotifier discordNotifier;

    private static final Long USER_ID = 10L;
    private static final Long INQUIRY_ID = 1L;
    private static final Long TARGET_ID = 100L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 8, 20, 0, 0);

    private Inquiry buildInquiry(Long id, InquiryType type, InquiryStatus status, String adminNote) {
        Inquiry inquiry = Inquiry.builder()
                .id(id)
                .userId(USER_ID)
                .type(type)
                .targetId(TARGET_ID)
                .title("공연 정보 오류 신고")
                .content("공연 날짜가 잘못 표기되어 있습니다.")
                .status(status)
                .adminNote(adminNote)
                .build();
        ReflectionTestUtils.setField(inquiry, "createdAt", CREATED_AT);
        return inquiry;
    }

    // -------------------------------------------------------------------------
    // createInquiry
    // -------------------------------------------------------------------------

    @Test
    void should_save_inquiry_when_concert_type_and_target_exists() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.CONCERT, TARGET_ID, "공연 정보 오류 신고", "공연 날짜가 잘못 표기되어 있습니다.");
        given(concertRepository.existsById(TARGET_ID)).willReturn(true);
        given(inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(USER_ID, TARGET_ID, InquiryType.CONCERT, InquiryStatus.PENDING))
                .willReturn(false);

        // when
        inquiryService.createInquiry(USER_ID, request);

        // then
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void should_save_inquiry_when_artist_type_and_target_exists() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.ARTIST, TARGET_ID, "아티스트 정보 오류 신고", "아티스트 이름이 잘못되어 있습니다.");
        given(artistRepository.existsById(TARGET_ID)).willReturn(true);
        given(inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(USER_ID, TARGET_ID, InquiryType.ARTIST, InquiryStatus.PENDING))
                .willReturn(false);

        // when
        inquiryService.createInquiry(USER_ID, request);

        // then
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void should_save_inquiry_when_setlist_type_uses_concert_repository() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.SETLIST, TARGET_ID, "셋리스트 정보 오류 신고", "셋리스트가 누락되어 있습니다.");
        given(concertRepository.existsById(TARGET_ID)).willReturn(true);
        given(inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(USER_ID, TARGET_ID, InquiryType.SETLIST, InquiryStatus.PENDING))
                .willReturn(false);

        // when
        inquiryService.createInquiry(USER_ID, request);

        // then
        verify(concertRepository).existsById(TARGET_ID);
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void should_throw_target_not_found_when_concert_does_not_exist() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.CONCERT, TARGET_ID, "공연 정보 오류 신고", "공연 날짜가 잘못 표기되어 있습니다.");
        given(concertRepository.existsById(TARGET_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> inquiryService.createInquiry(USER_ID, request))
                .isInstanceOf(TargetNotFoundException.class)
                .hasMessage(ErrorCode.TARGET_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_target_not_found_when_artist_does_not_exist() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.ARTIST, TARGET_ID, "아티스트 정보 오류 신고", "아티스트 이름이 잘못되어 있습니다.");
        given(artistRepository.existsById(TARGET_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> inquiryService.createInquiry(USER_ID, request))
                .isInstanceOf(TargetNotFoundException.class)
                .hasMessage(ErrorCode.TARGET_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_inquiry_already_pending_when_duplicate_pending_exists() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.CONCERT, TARGET_ID, "공연 정보 오류 신고", "공연 날짜가 잘못 표기되어 있습니다.");
        given(concertRepository.existsById(TARGET_ID)).willReturn(true);
        given(inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(USER_ID, TARGET_ID, InquiryType.CONCERT, InquiryStatus.PENDING))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> inquiryService.createInquiry(USER_ID, request))
                .isInstanceOf(InquiryAlreadyPendingException.class)
                .hasMessage(ErrorCode.INQUIRY_ALREADY_PENDING.getMessage());
    }

    @Test
    void should_save_inquiry_when_data_request_type_without_target_validation() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.DATA_REQUEST, null, "아티스트 추가 요청", "홍길동 아티스트를 추가해주세요.");

        // when
        inquiryService.createInquiry(USER_ID, request);

        // then
        verify(concertRepository, org.mockito.Mockito.never()).existsById(any());
        verify(artistRepository, org.mockito.Mockito.never()).existsById(any());
        verify(inquiryRepository, org.mockito.Mockito.never()).existsByUserIdAndTargetIdAndTypeAndStatus(any(), any(), any(), any());
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void should_save_inquiry_when_feedback_type_without_target_validation() {
        // given
        InquiryCreateRequest request = new InquiryCreateRequest(
                InquiryType.FEEDBACK, null, "서비스 피드백", "앱 사용이 불편합니다.");

        // when
        inquiryService.createInquiry(USER_ID, request);

        // then
        verify(concertRepository, org.mockito.Mockito.never()).existsById(any());
        verify(artistRepository, org.mockito.Mockito.never()).existsById(any());
        verify(inquiryRepository, org.mockito.Mockito.never()).existsByUserIdAndTargetIdAndTypeAndStatus(any(), any(), any(), any());
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    // -------------------------------------------------------------------------
    // getMyInquiries
    // -------------------------------------------------------------------------

    @Test
    void should_return_all_inquiries_when_status_is_null() {
        // given
        Inquiry inquiry = buildInquiry(INQUIRY_ID, InquiryType.CONCERT, InquiryStatus.PENDING, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findByUserId(USER_ID, PAGEABLE)).willReturn(page);

        // when
        PageResponse<InquiryListItemResponse> response = inquiryService.getMyInquiries(USER_ID, null, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(INQUIRY_ID);
        assertThat(response.content().get(0).status()).isEqualTo(InquiryStatus.PENDING);
        assertThat(response.content().get(0).resultMessage()).isNull();
        assertThat(response.content().get(0).rejectReason()).isNull();
        verify(inquiryRepository).findByUserId(USER_ID, PAGEABLE);
    }

    @Test
    void should_return_filtered_inquiries_when_status_is_given() {
        // given
        Inquiry inquiry = buildInquiry(INQUIRY_ID, InquiryType.CONCERT, InquiryStatus.IN_PROGRESS, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findByUserIdAndStatus(USER_ID, InquiryStatus.IN_PROGRESS, PAGEABLE)).willReturn(page);

        // when
        PageResponse<InquiryListItemResponse> response = inquiryService.getMyInquiries(USER_ID, InquiryStatus.IN_PROGRESS, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo(InquiryStatus.IN_PROGRESS);
        verify(inquiryRepository).findByUserIdAndStatus(USER_ID, InquiryStatus.IN_PROGRESS, PAGEABLE);
    }

    @Test
    void should_return_result_message_when_status_is_resolved() {
        // given
        Inquiry inquiry = buildInquiry(INQUIRY_ID, InquiryType.CONCERT, InquiryStatus.RESOLVED, "수정 완료되었습니다.");
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findByUserId(USER_ID, PAGEABLE)).willReturn(page);

        // when
        PageResponse<InquiryListItemResponse> response = inquiryService.getMyInquiries(USER_ID, null, PAGEABLE);

        // then
        assertThat(response.content().get(0).resultMessage()).isEqualTo("수정 완료되었습니다.");
        assertThat(response.content().get(0).rejectReason()).isNull();
    }

    @Test
    void should_return_reject_reason_when_status_is_rejected() {
        // given
        Inquiry inquiry = buildInquiry(INQUIRY_ID, InquiryType.ARTIST, InquiryStatus.REJECTED, "정확한 정보입니다.");
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findByUserId(USER_ID, PAGEABLE)).willReturn(page);

        // when
        PageResponse<InquiryListItemResponse> response = inquiryService.getMyInquiries(USER_ID, null, PAGEABLE);

        // then
        assertThat(response.content().get(0).rejectReason()).isEqualTo("정확한 정보입니다.");
        assertThat(response.content().get(0).resultMessage()).isNull();
    }

    // -------------------------------------------------------------------------
    // getMyInquiryDetail
    // -------------------------------------------------------------------------

    @Test
    void should_return_inquiry_detail_when_found() {
        // given
        Inquiry inquiry = buildInquiry(INQUIRY_ID, InquiryType.CONCERT, InquiryStatus.PENDING, null);
        given(inquiryRepository.findByIdAndUserId(INQUIRY_ID, USER_ID)).willReturn(Optional.of(inquiry));

        // when
        InquiryDetailResponse response = inquiryService.getMyInquiryDetail(USER_ID, INQUIRY_ID);

        // then
        assertThat(response.id()).isEqualTo(INQUIRY_ID);
        assertThat(response.type()).isEqualTo(InquiryType.CONCERT);
        assertThat(response.title()).isEqualTo("공연 정보 오류 신고");
        assertThat(response.content()).isEqualTo("공연 날짜가 잘못 표기되어 있습니다.");
        assertThat(response.status()).isEqualTo(InquiryStatus.PENDING);
        assertThat(response.createdAt()).isEqualTo("2025-08-20");
        assertThat(response.resultMessage()).isNull();
        assertThat(response.rejectReason()).isNull();
    }

    @Test
    void should_throw_inquiry_not_found_when_not_found_or_not_owner() {
        // given
        given(inquiryRepository.findByIdAndUserId(INQUIRY_ID, USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.getMyInquiryDetail(USER_ID, INQUIRY_ID))
                .isInstanceOf(InquiryNotFoundException.class)
                .hasMessage(ErrorCode.INQUIRY_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // existsPendingInquiry
    // -------------------------------------------------------------------------

    @Test
    void should_return_exists_true_when_pending_inquiry_exists() {
        // given
        given(inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(
                USER_ID, TARGET_ID, InquiryType.CONCERT, InquiryStatus.PENDING)).willReturn(true);

        // when
        InquiryExistsResponse response = inquiryService.existsPendingInquiry(USER_ID, InquiryType.CONCERT, TARGET_ID);

        // then
        assertThat(response.exists()).isTrue();
    }

    @Test
    void should_return_exists_false_when_no_pending_inquiry() {
        // given
        given(inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(
                USER_ID, TARGET_ID, InquiryType.ARTIST, InquiryStatus.PENDING)).willReturn(false);

        // when
        InquiryExistsResponse response = inquiryService.existsPendingInquiry(USER_ID, InquiryType.ARTIST, TARGET_ID);

        // then
        assertThat(response.exists()).isFalse();
    }

    @Test
    void should_return_exists_false_for_data_request_type_without_repo_call() {
        // when
        InquiryExistsResponse response = inquiryService.existsPendingInquiry(USER_ID, InquiryType.DATA_REQUEST, null);

        // then
        assertThat(response.exists()).isFalse();
        verify(inquiryRepository, org.mockito.Mockito.never()).existsByUserIdAndTargetIdAndTypeAndStatus(any(), any(), any(), any());
    }

    @Test
    void should_return_exists_false_for_feedback_type_without_repo_call() {
        // when
        InquiryExistsResponse response = inquiryService.existsPendingInquiry(USER_ID, InquiryType.FEEDBACK, null);

        // then
        assertThat(response.exists()).isFalse();
        verify(inquiryRepository, org.mockito.Mockito.never()).existsByUserIdAndTargetIdAndTypeAndStatus(any(), any(), any(), any());
    }
}
