package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.dto.AdminArtistCreateRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateRequest;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.BookingLinkRequest;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertBookingLink;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.entity.ConcertStatusLog;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertBookingLinkRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.concert.repository.ConcertStatusLogRepository;
import com.Coming.Backend.concert.entity.Setlist;
import com.Coming.Backend.concert.repository.SetlistRepository;
import com.Coming.Backend.concert.repository.SetlistTrackRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.any;
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
    private ConcertBookingLinkRepository concertBookingLinkRepository;

    @Mock
    private ConcertStatusLogRepository concertStatusLogRepository;

    @Mock
    private SetlistRepository setlistRepository;

    @Mock
    private SetlistTrackRepository setlistTrackRepository;

    @Mock
    private UserConcertCalendarRepository userConcertCalendarRepository;

    private static final Long USER_ID = 10L;
    private static final Long INQUIRY_ID = 1L;
    private static final Long TARGET_ID = 100L;
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
    void should_save_artist_when_create_request_given() {
        // given
        AdminArtistCreateRequest request = new AdminArtistCreateRequest(
                "some-mbid-123",
                "IU",
                "IU"
        );
        given(artistRepository.save(any(Artist.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        adminService.createArtist(request);

        // then
        ArgumentCaptor<Artist> captor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(captor.capture());

        Artist saved = captor.getValue();
        assertThat(saved.getMbid()).isEqualTo("some-mbid-123");
        assertThat(saved.getName()).isEqualTo("IU");
        assertThat(saved.getSortName()).isEqualTo("IU");
        assertThat(saved.isComing()).isFalse();
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
    // createConcert
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
                .venueAddress("서울시 송파구")
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
    void should_save_concert_when_create_request_given() {
        // given
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "PF123456", "아이유 콘서트", "아이유",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30),
                "올림픽공원 체조경기장", "서울시 송파구",
                "https://example.com/poster.jpg", "VIP 150,000원",
                ConcertStatus.UPCOMING, null
        );
        Concert savedConcert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.save(any(Concert.class))).willReturn(savedConcert);

        // when
        adminService.createConcert(request);

        // then
        ArgumentCaptor<Concert> captor = ArgumentCaptor.forClass(Concert.class);
        verify(concertRepository).save(captor.capture());
        Concert saved = captor.getValue();
        assertThat(saved.getKopisId()).isEqualTo("PF123456");
        assertThat(saved.getTitle()).isEqualTo("아이유 콘서트");
        assertThat(saved.getStatus()).isEqualTo(ConcertStatus.UPCOMING);
        assertThat(saved.getViewCount()).isEqualTo(0L);
        verify(concertArtistRepository, never()).saveAll(any());
    }

    @Test
    void should_save_concert_artist_mappings_when_artist_ids_given() {
        // given
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "PF123456", "아이유 콘서트", "아이유",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30),
                "올림픽공원 체조경기장", null, null, null,
                ConcertStatus.UPCOMING, List.of(1L, 2L)
        );
        Concert savedConcert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.save(any(Concert.class))).willReturn(savedConcert);

        // when
        adminService.createConcert(request);

        // then
        ArgumentCaptor<List<ConcertArtist>> captor = ArgumentCaptor.forClass(List.class);
        verify(concertArtistRepository).saveAll(captor.capture());
        List<ConcertArtist> mappings = captor.getValue();
        assertThat(mappings).hasSize(2);
        assertThat(mappings.get(0).getArtistId()).isEqualTo(1L);
        assertThat(mappings.get(1).getArtistId()).isEqualTo(2L);
        assertThat(mappings.get(0).getConfidence()).isEqualTo("HIGH");
        assertThat(mappings.get(0).getMatchedBy()).isEqualTo("ADMIN");
    }

    // -------------------------------------------------------------------------
    // updateConcert
    // -------------------------------------------------------------------------

    @Test
    void should_update_concert_fields_when_update_request_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                "아이유 앙코르 콘서트", null,
                LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31),
                null, null, null, "R석 100,000원", null
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
                null, null, null, null, null, null, null, null, links
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
                "새 제목", null, null, null, null, null, null, null, null
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
                "새 제목", null, null, null, null, null, null, null, null
        );
        given(concertRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateConcert(999L, request))
                .isInstanceOf(ConcertNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deleteConcert
    // -------------------------------------------------------------------------

    @Test
    void should_delete_concert_and_all_related_data_when_valid_id_given() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());

        // when
        adminService.deleteConcert(CONCERT_ID);

        // then
        verify(concertStatusLogRepository).deleteByConcertId(CONCERT_ID);
        verify(setlistTrackRepository, never()).deleteBySetlistIdIn(any());
        verify(setlistRepository).deleteAllById(List.of());
        verify(userConcertCalendarRepository).deleteByConcertId(CONCERT_ID);
        verify(concertArtistRepository).deleteByConcertId(CONCERT_ID);
        verify(concertBookingLinkRepository).deleteByConcertId(CONCERT_ID);
        verify(concertRepository).deleteById(CONCERT_ID);
    }

    @Test
    void should_delete_setlist_tracks_when_setlists_exist() {
        // given
        Setlist setlist = Setlist.builder()
                .setlistFmId("setlist-fm-id")
                .concertId(CONCERT_ID)
                .collectedAt(LocalDateTime.of(2025, 9, 1, 20, 0))
                .build();
        ReflectionTestUtils.setField(setlist, "id", 10L);
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(setlist));

        // when
        adminService.deleteConcert(CONCERT_ID);

        // then
        verify(setlistTrackRepository).deleteBySetlistIdIn(List.of(10L));
        verify(setlistRepository).deleteAllById(List.of(10L));
    }

    @Test
    void should_throw_concert_not_found_when_delete_target_does_not_exist() {
        // given
        given(concertRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.deleteConcert(999L))
                .isInstanceOf(ConcertNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // forceChangeConcertState
    // -------------------------------------------------------------------------

    @Test
    void should_change_status_and_save_log_when_valid_request_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertStateUpdateRequest request = new AdminConcertStateUpdateRequest(
                ConcertStatus.CANCELLED, "공연 취소"
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertStatusLogRepository.save(any(ConcertStatusLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        adminService.forceChangeConcertState(CONCERT_ID, request);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.CANCELLED);
        ArgumentCaptor<ConcertStatusLog> captor = ArgumentCaptor.forClass(ConcertStatusLog.class);
        verify(concertStatusLogRepository).save(captor.capture());
        ConcertStatusLog log = captor.getValue();
        assertThat(log.getConcertId()).isEqualTo(CONCERT_ID);
        assertThat(log.getBeforeStatus()).isEqualTo(ConcertStatus.UPCOMING);
        assertThat(log.getAfterStatus()).isEqualTo(ConcertStatus.CANCELLED);
        assertThat(log.getReason()).isEqualTo("공연 취소");
        assertThat(log.getChangedAt()).isNotNull();
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
}
