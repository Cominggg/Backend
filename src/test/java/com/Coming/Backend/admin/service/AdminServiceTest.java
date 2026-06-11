package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.client.DataPipelineClient;
import com.Coming.Backend.admin.dto.AdminArtistCollectRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCollectRequest;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.AdminPendingConcertResponse;
import com.Coming.Backend.admin.dto.BookingLinkRequest;
import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertArtistCandidate;
import com.Coming.Backend.concert.entity.ConcertBookingLink;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertArtistAlreadyExistsException;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.exception.ConcertNotPendingException;
import com.Coming.Backend.concert.repository.ConcertArtistCandidateRepository;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertBookingLinkRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.Coming.Backend.inquiry.exception.InvalidInquiryStatusException;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ConcertArtistRepository concertArtistRepository;

    @Mock
    private ConcertArtistCandidateRepository concertArtistCandidateRepository;

    @Mock
    private ConcertBookingLinkRepository concertBookingLinkRepository;

    @Mock
    private DataPipelineClient dataPipelineClient;

    private static final Long USER_ID = 10L;
    private static final Long INQUIRY_ID = 1L;
    private static final Long TARGET_ID = 100L;
    private static final Long ARTIST_ID = 20L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 8, 20, 0, 0);

    private Inquiry buildInquiry(InquiryType type, InquiryStatus status, String adminNote) {
        Inquiry inquiry = Inquiry.builder()
                .id(INQUIRY_ID)
                .userId(USER_ID)
                .type(type)
                .targetId(TARGET_ID)
                .title("아티스트 정보 오류 신고")
                .content("아티스트 이름이 잘못되어 있습니다.")
                .status(status)
                .adminNote(adminNote)
                .build();
        ReflectionTestUtils.setField(inquiry, "createdAt", CREATED_AT);
        return inquiry;
    }

    private User buildUser(Long id, String nickname) {
        return User.builder()
                .id(id)
                .provider("google")
                .providerId("provider-id-" + id)
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void should_update_all_fields_when_full_update_request_given() {
        // given
        Artist artist = Artist.builder()
                .mbid("some-mbid-123")
                .name("IU")
                .sortName("IU")
                .isComing(false)
                .build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest(
                "아이유",
                "Iu, Lee Ji Eun"
        );
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        assertThat(artist.getName()).isEqualTo("아이유");
        assertThat(artist.getSortName()).isEqualTo("Iu, Lee Ji Eun");
    }

    @Test
    void should_not_overwrite_null_fields_when_partial_update_request_given() {
        // given
        Artist artist = Artist.builder()
                .mbid("some-mbid-123")
                .name("IU")
                .sortName("IU")
                .isComing(false)
                .build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest("아이유", null);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        assertThat(artist.getName()).isEqualTo("아이유");
        assertThat(artist.getSortName()).isEqualTo("IU");
    }

    @Test
    void should_throw_artist_not_found_exception_when_invalid_id_given() {
        // given
        given(artistRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateArtist(999L, new AdminArtistUpdateRequest("IU", null)))
                .isInstanceOf(ArtistNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getInquiries
    // -------------------------------------------------------------------------

    @Test
    void should_call_find_all_when_type_and_status_are_null() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findAll(PAGEABLE)).willReturn(page);
        given(userRepository.findAllByIdIn(List.of(USER_ID))).willReturn(List.of(buildUser(USER_ID, "닉네임")));

        // when
        PageResponse<AdminInquiryListItemResponse> response = adminService.getInquiries(null, null, PAGEABLE);

        // then
        verify(inquiryRepository).findAll(PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_find_all_by_type_when_only_type_given() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findAllByType(InquiryType.ARTIST, PAGEABLE)).willReturn(page);
        given(userRepository.findAllByIdIn(List.of(USER_ID))).willReturn(List.of(buildUser(USER_ID, "닉네임")));

        // when
        PageResponse<AdminInquiryListItemResponse> response = adminService.getInquiries(InquiryType.ARTIST, null, PAGEABLE);

        // then
        verify(inquiryRepository).findAllByType(InquiryType.ARTIST, PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_find_all_by_status_when_only_status_given() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findAllByStatus(InquiryStatus.PENDING, PAGEABLE)).willReturn(page);
        given(userRepository.findAllByIdIn(List.of(USER_ID))).willReturn(List.of(buildUser(USER_ID, "닉네임")));

        // when
        PageResponse<AdminInquiryListItemResponse> response = adminService.getInquiries(null, InquiryStatus.PENDING, PAGEABLE);

        // then
        verify(inquiryRepository).findAllByStatus(InquiryStatus.PENDING, PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_find_all_by_type_and_status_when_both_given() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findAllByTypeAndStatus(InquiryType.ARTIST, InquiryStatus.PENDING, PAGEABLE)).willReturn(page);
        given(userRepository.findAllByIdIn(List.of(USER_ID))).willReturn(List.of(buildUser(USER_ID, "닉네임")));

        // when
        PageResponse<AdminInquiryListItemResponse> response = adminService.getInquiries(InquiryType.ARTIST, InquiryStatus.PENDING, PAGEABLE);

        // then
        verify(inquiryRepository).findAllByTypeAndStatus(InquiryType.ARTIST, InquiryStatus.PENDING, PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_map_user_nickname_correctly_when_inquiries_returned() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry), PAGEABLE, 1);
        given(inquiryRepository.findAll(PAGEABLE)).willReturn(page);
        given(userRepository.findAllByIdIn(List.of(USER_ID))).willReturn(List.of(buildUser(USER_ID, "아이유팬")));

        // when
        PageResponse<AdminInquiryListItemResponse> response = adminService.getInquiries(null, null, PAGEABLE);

        // then
        assertThat(response.content().get(0).userNickname()).isEqualTo("아이유팬");
        assertThat(response.content().get(0).userId()).isEqualTo(USER_ID);
    }

    // -------------------------------------------------------------------------
    // getInquiryDetail
    // -------------------------------------------------------------------------

    @Test
    void should_return_inquiry_detail_when_valid_id_given() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.RESOLVED, "정보가 수정되었습니다.");
        User user = buildUser(USER_ID, "아이유팬");
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.of(inquiry));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        AdminInquiryDetailResponse response = adminService.getInquiryDetail(INQUIRY_ID);

        // then
        assertThat(response.id()).isEqualTo(INQUIRY_ID);
        assertThat(response.type()).isEqualTo(InquiryType.ARTIST);
        assertThat(response.status()).isEqualTo(InquiryStatus.RESOLVED);
        assertThat(response.content()).isEqualTo("아티스트 이름이 잘못되어 있습니다.");
        assertThat(response.targetId()).isEqualTo(TARGET_ID);
        assertThat(response.userNickname()).isEqualTo("아이유팬");
        assertThat(response.resultMessage()).isEqualTo("정보가 수정되었습니다.");
        assertThat(response.rejectReason()).isNull();
        assertThat(response.createdAt()).isEqualTo("2025-08-20");
    }

    @Test
    void should_throw_inquiry_not_found_when_inquiry_id_does_not_exist() {
        // given
        given(inquiryRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.getInquiryDetail(999L))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updateInquiryStatus
    // -------------------------------------------------------------------------

    @Test
    void should_update_status_when_valid_id_and_request_given() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        AdminInquiryStatusUpdateRequest request = new AdminInquiryStatusUpdateRequest(InquiryStatus.RESOLVED, "처리 완료");
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.of(inquiry));

        // when
        adminService.updateInquiryStatus(INQUIRY_ID, request);

        // then
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RESOLVED);
        assertThat(inquiry.getAdminNote()).isEqualTo("처리 완료");
    }

    @Test
    void should_save_admin_note_when_status_is_rejected() {
        // given
        Inquiry inquiry = buildInquiry(InquiryType.ARTIST, InquiryStatus.PENDING, null);
        AdminInquiryStatusUpdateRequest request = new AdminInquiryStatusUpdateRequest(InquiryStatus.REJECTED, "정확한 정보입니다.");
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.of(inquiry));

        // when
        adminService.updateInquiryStatus(INQUIRY_ID, request);

        // then
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.REJECTED);
        assertThat(inquiry.getAdminNote()).isEqualTo("정확한 정보입니다.");
    }

    @Test
    void should_throw_inquiry_not_found_when_update_target_does_not_exist() {
        // given
        AdminInquiryStatusUpdateRequest request = new AdminInquiryStatusUpdateRequest(InquiryStatus.RESOLVED, null);
        given(inquiryRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateInquiryStatus(999L, request))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    @Test
    void should_throw_invalid_inquiry_status_when_status_is_pending() {
        // given
        AdminInquiryStatusUpdateRequest request = new AdminInquiryStatusUpdateRequest(InquiryStatus.PENDING, null);

        // when & then
        assertThatThrownBy(() -> adminService.updateInquiryStatus(INQUIRY_ID, request))
                .isInstanceOf(InvalidInquiryStatusException.class);
    }

    // -------------------------------------------------------------------------
    // updateConcert
    // -------------------------------------------------------------------------

    private static final Long CONCERT_ID = 1L;

    private Concert buildConcert(Long id, ConcertStatus status) {
        Concert concert = Concert.builder()
                .kopisId("PF123456")
                .title("아이유 콘서트")
                .cast("아이유")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 30))
                .venueName("올림픽공원 체조경기장")
                .posterUrl("https://example.com/poster.jpg")
                .price("VIP 150,000원")
                .status(status)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.of(2025, 9, 1))
                .build();
        ReflectionTestUtils.setField(concert, "id", id);
        return concert;
    }

    @Test
    void should_update_concert_fields_when_update_request_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                "아이유 앙코르 콘서트", null,
                LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31),
                null, null, "R석 100,000원", null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        assertThat(concert.getTitle()).isEqualTo("아이유 앙코르 콘서트");
        assertThat(concert.getStartDate()).isEqualTo(LocalDate.of(2025, 10, 1));
        assertThat(concert.getPrice()).isEqualTo("R석 100,000원");
    }

    @Test
    void should_replace_booking_links_when_booking_links_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        List<BookingLinkRequest> links = List.of(
                new BookingLinkRequest("인터파크", "https://interpark.com/ticket/1"),
                new BookingLinkRequest("YES24", "https://yes24.com/ticket/1")
        );
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                null, null, null, null, null, null, null, links
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        verify(concertBookingLinkRepository).deleteByConcertId(CONCERT_ID);
        ArgumentCaptor<List<ConcertBookingLink>> captor = ArgumentCaptor.forClass(List.class);
        verify(concertBookingLinkRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getName()).isEqualTo("인터파크");
        assertThat(captor.getValue().get(1).getName()).isEqualTo("YES24");
    }

    @Test
    void should_not_touch_booking_links_when_booking_links_is_null() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                "새 제목", null, null, null, null, null, null, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        verify(concertBookingLinkRepository, never()).deleteByConcertId(any());
        verify(concertBookingLinkRepository, never()).saveAll(any());
    }

    @Test
    void should_throw_concert_not_found_when_update_target_does_not_exist() {
        // given
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                "새 제목", null, null, null, null, null, null, null
        );
        given(concertRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateConcert(999L, request))
                .isInstanceOf(ConcertNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // forceChangeConcertState
    // -------------------------------------------------------------------------

    @Test
    void should_change_status_when_valid_request_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertStateUpdateRequest request = new AdminConcertStateUpdateRequest(
                ConcertStatus.CANCELLED, "공연 취소"
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.forceChangeConcertState(CONCERT_ID, request);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.CANCELLED);
    }

    @Test
    void should_throw_concert_not_found_when_state_change_target_does_not_exist() {
        // given
        AdminConcertStateUpdateRequest request = new AdminConcertStateUpdateRequest(
                ConcertStatus.CANCELLED, null
        );
        given(concertRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.forceChangeConcertState(999L, request))
                .isInstanceOf(ConcertNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getPendingConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_pending_concerts_with_candidates_when_pending_concerts_exist() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).matchedBy("kopis").build();

        given(concertRepository.findByStatus(ConcertStatus.PENDING, PAGEABLE)).willReturn(page);
        given(concertArtistCandidateRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<AdminPendingConcertResponse> response = adminService.getPendingConcerts(PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(CONCERT_ID);
        assertThat(response.content().get(0).candidates()).hasSize(1);
        assertThat(response.content().get(0).candidates().get(0).artistId()).isEqualTo(ARTIST_ID);
        assertThat(response.content().get(0).candidates().get(0).name()).isEqualTo("YOASOBI");
        assertThat(response.content().get(0).candidates().get(0).matchedBy()).isEqualTo("kopis");
    }

    @Test
    void should_return_empty_candidate_list_when_concert_has_no_candidates() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByStatus(ConcertStatus.PENDING, PAGEABLE)).willReturn(page);
        given(concertArtistCandidateRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());
        given(artistRepository.findAllById(List.of())).willReturn(List.of());

        // when
        PageResponse<AdminPendingConcertResponse> response = adminService.getPendingConcerts(PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).candidates()).isEmpty();
    }

    @Test
    void should_return_empty_page_when_no_pending_concerts_exist() {
        // given
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findByStatus(ConcertStatus.PENDING, PAGEABLE)).willReturn(emptyPage);

        // when
        PageResponse<AdminPendingConcertResponse> response = adminService.getPendingConcerts(PAGEABLE);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    // -------------------------------------------------------------------------
    // approveConcert
    // -------------------------------------------------------------------------

    @Test
    void should_move_candidates_to_concert_artist_and_set_status_when_approved() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        ReflectionTestUtils.setField(concert, "startDate", LocalDate.now().plusDays(10));
        ReflectionTestUtils.setField(concert, "endDate", LocalDate.now().plusDays(12));
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).matchedBy("kopis").build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.approveConcert(CONCERT_ID);

        // then
        verify(concertArtistRepository).saveAll(any());
        verify(concertArtistCandidateRepository).deleteByConcertId(CONCERT_ID);
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.UPCOMING);
        assertThat(artist.isComing()).isTrue();
    }

    @Test
    void should_set_status_ended_when_approved_concert_dates_are_in_the_past() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        ReflectionTestUtils.setField(concert, "startDate", LocalDate.now().minusDays(5));
        ReflectionTestUtils.setField(concert, "endDate", LocalDate.now().minusDays(3));
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).matchedBy("kopis").build();
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(false);

        // when
        adminService.approveConcert(CONCERT_ID);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.ENDED);
        assertThat(artist.isComing()).isFalse();
    }

    @Test
    void should_throw_concert_not_pending_when_approving_non_pending_concert() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> adminService.approveConcert(CONCERT_ID))
                .isInstanceOf(ConcertNotPendingException.class);
    }

    // -------------------------------------------------------------------------
    // rejectConcert
    // -------------------------------------------------------------------------

    @Test
    void should_set_status_excluded_and_delete_candidates_when_rejected() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.rejectConcert(CONCERT_ID);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.EXCLUDED);
        verify(concertArtistCandidateRepository).deleteByConcertId(CONCERT_ID);
        verify(concertArtistRepository).deleteByConcertId(CONCERT_ID);
    }

    @Test
    void should_throw_concert_not_pending_when_rejecting_non_pending_concert() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> adminService.rejectConcert(CONCERT_ID))
                .isInstanceOf(ConcertNotPendingException.class);
    }

    // -------------------------------------------------------------------------
    // assignArtistToConcert
    // -------------------------------------------------------------------------

    @Test
    void should_save_concert_artist_and_set_is_coming_true_when_concert_is_upcoming() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(concertArtistRepository.existsByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID)).willReturn(false);

        // when
        adminService.assignArtistToConcert(CONCERT_ID, ARTIST_ID);

        // then
        verify(concertArtistRepository).save(any());
        assertThat(artist.isComing()).isTrue();
    }

    @Test
    void should_not_update_is_coming_when_concert_is_ended() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.ENDED);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(concertArtistRepository.existsByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID)).willReturn(false);

        // when
        adminService.assignArtistToConcert(CONCERT_ID, ARTIST_ID);

        // then
        verify(concertArtistRepository).save(any());
        assertThat(artist.isComing()).isFalse();
    }

    @Test
    void should_throw_concert_artist_already_exists_when_mapping_already_exists() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(true).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(concertArtistRepository.existsByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminService.assignArtistToConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertArtistAlreadyExistsException.class);
        verify(concertArtistRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // searchArtists
    // -------------------------------------------------------------------------

    @Test
    void should_return_artist_candidates_when_name_given() {
        // given
        List<DataArtistSearchResult> results = List.of(
                new DataArtistSearchResult("mbid-1", "IU", "KR", "Person", "https://musicbrainz.org/artist/mbid-1"),
                new DataArtistSearchResult("mbid-2", "IU (instrumental)", "JP", null, null)
        );
        given(dataPipelineClient.searchArtists("IU")).willReturn(results);

        // when
        List<DataArtistSearchResult> response = adminService.searchArtists("IU");

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).mbid()).isEqualTo("mbid-1");
        assertThat(response.get(0).name()).isEqualTo("IU");
        assertThat(response.get(0).country()).isEqualTo("KR");
        assertThat(response.get(0).type()).isEqualTo("Person");
        assertThat(response.get(1).type()).isNull();
    }

    @Test
    void should_return_empty_list_when_no_artists_match() {
        // given
        given(dataPipelineClient.searchArtists("존재하지않는아티스트")).willReturn(List.of());

        // when
        List<DataArtistSearchResult> response = adminService.searchArtists("존재하지않는아티스트");

        // then
        assertThat(response).isEmpty();
    }

    // -------------------------------------------------------------------------
    // searchConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_concert_candidates_when_title_given() {
        // given
        List<DataConcertSearchResult> results = List.of(
                new DataConcertSearchResult("PF001", "아이유 콘서트", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2), "올림픽공원 체조경기장", "https://kopis.or.kr/PF001"),
                new DataConcertSearchResult("PF002", "아이유 콘서트 앙코르", LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 6), "KSPO DOME", "https://kopis.or.kr/PF002")
        );
        given(dataPipelineClient.searchConcerts("아이유")).willReturn(results);

        // when
        List<DataConcertSearchResult> response = adminService.searchConcerts("아이유");

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).kopisId()).isEqualTo("PF001");
        assertThat(response.get(0).title()).isEqualTo("아이유 콘서트");
        assertThat(response.get(0).startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.get(0).venue()).isEqualTo("올림픽공원 체조경기장");
    }

    @Test
    void should_return_empty_list_when_no_concerts_match() {
        // given
        given(dataPipelineClient.searchConcerts("존재하지않는공연")).willReturn(List.of());

        // when
        List<DataConcertSearchResult> response = adminService.searchConcerts("존재하지않는공연");

        // then
        assertThat(response).isEmpty();
    }

    // -------------------------------------------------------------------------
    // collectArtist
    // -------------------------------------------------------------------------

    @Test
    void should_trigger_artist_collect_when_mbid_given() {
        // given
        AdminArtistCollectRequest request = new AdminArtistCollectRequest("some-mbid-123");

        // when
        adminService.collectArtist(request);

        // then
        verify(dataPipelineClient).triggerArtistCollect("some-mbid-123");
    }

    // -------------------------------------------------------------------------
    // collectConcert
    // -------------------------------------------------------------------------

    @Test
    void should_trigger_concert_collect_when_kopis_id_given() {
        // given
        AdminConcertCollectRequest request = new AdminConcertCollectRequest("PF123456");

        // when
        adminService.collectConcert(request);

        // then
        verify(dataPipelineClient).triggerConcertCollectByKopisId("PF123456");
    }
}
