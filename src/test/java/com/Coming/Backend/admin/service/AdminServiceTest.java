package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.client.DataPipelineClient;
import com.Coming.Backend.admin.dto.AdminArtistCollectRequest;
import com.Coming.Backend.admin.dto.AdminArtistSearchResult;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertApproveRequest;
import com.Coming.Backend.admin.dto.AdminConcertCollectRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateResponse;
import com.Coming.Backend.admin.dto.AdminConcertDetailResponse;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminExcludedConcertResponse;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.AdminPendingConcertResponse;
import com.Coming.Backend.admin.dto.BookingLinkRequest;
import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import com.Coming.Backend.admin.dto.PipelineArtistCollectResult;
import com.Coming.Backend.admin.dto.PipelineConcertCollectResult;
import com.Coming.Backend.admin.dto.AdminArtistDetailResponse;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
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
import com.Coming.Backend.concert.entity.ConcertImage;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertAlreadyExistsException;
import com.Coming.Backend.concert.exception.ConcertArtistAlreadyExistsException;
import com.Coming.Backend.concert.exception.ConcertArtistNotFoundException;
import com.Coming.Backend.concert.exception.ConcertIsPendingException;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.exception.ConcertNotPendingException;
import com.Coming.Backend.concert.repository.ConcertArtistCandidateRepository;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertBookingLinkRepository;
import com.Coming.Backend.concert.repository.ConcertImageRepository;
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
    private ArtistAliasRepository artistAliasRepository;

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
    private ConcertImageRepository concertImageRepository;

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
                "Iu, Lee Ji Eun",
                null
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
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest("아이유", null, null);
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
        assertThatThrownBy(() -> adminService.updateArtist(999L, new AdminArtistUpdateRequest("IU", null, null)))
                .isInstanceOf(ArtistNotFoundException.class);
    }

    @Test
    void should_only_insert_new_aliases_and_not_reinsert_existing_ones() {
        // given - DB: ja=["ヨアソビ"], 요청: ja=["ヨアソビ", "よあそび"] → "よあそび"만 추가
        Artist artist = Artist.builder().mbid("mbid-1").name("YOASOBI").isComing(false).build();
        ArtistAlias existingJa = ArtistAlias.builder().artistId(1L).name("ヨアソビ").locale("ja").build();
        AdminArtistUpdateRequest.AliasesRequest aliases = new AdminArtistUpdateRequest.AliasesRequest(
                List.of("ヨアソビ", "よあそび"), List.of(), List.of());
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest(null, null, aliases);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistAliasRepository.findByArtistId(1L)).willReturn(List.of(existingJa));

        // when
        adminService.updateArtist(1L, request);

        // then - 기존 "ヨアソビ"는 건드리지 않고, "よあそび"만 신규 저장
        verify(artistAliasRepository, never()).deleteAll(any(List.class));
        ArgumentCaptor<List<ArtistAlias>> captor = ArgumentCaptor.forClass(List.class);
        verify(artistAliasRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getName()).isEqualTo("よあそび");
        assertThat(captor.getValue().get(0).getLocale()).isEqualTo("ja");
    }

    @Test
    void should_only_delete_removed_aliases_when_alias_removed_from_list() {
        // given - DB: ja=["ヨアソビ", "よあそび"], 요청: ja=["ヨアソビ"] → "よあそび"만 삭제
        Artist artist = Artist.builder().mbid("mbid-1").name("YOASOBI").isComing(false).build();
        ArtistAlias alias1 = ArtistAlias.builder().artistId(1L).name("ヨアソビ").locale("ja").build();
        ArtistAlias alias2 = ArtistAlias.builder().artistId(1L).name("よあそび").locale("ja").build();
        AdminArtistUpdateRequest.AliasesRequest aliases = new AdminArtistUpdateRequest.AliasesRequest(
                List.of("ヨアソビ"), List.of(), List.of());
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest(null, null, aliases);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistAliasRepository.findByArtistId(1L)).willReturn(List.of(alias1, alias2));

        // when
        adminService.updateArtist(1L, request);

        // then - "よあそび"만 삭제, "ヨアソビ"는 그대로 유지
        ArgumentCaptor<List<ArtistAlias>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(artistAliasRepository).deleteAll(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).hasSize(1);
        assertThat(deleteCaptor.getValue().get(0).getName()).isEqualTo("よあそび");
        verify(artistAliasRepository, never()).saveAll(any());
    }

    @Test
    void should_delete_all_locale_aliases_when_empty_list_given() {
        // given - DB: ja=["ヨアソビ"], 요청: ja=[] → 전체 삭제
        Artist artist = Artist.builder().mbid("mbid-1").name("YOASOBI").isComing(false).build();
        ArtistAlias existing = ArtistAlias.builder().artistId(1L).name("ヨアソビ").locale("ja").build();
        AdminArtistUpdateRequest.AliasesRequest aliases = new AdminArtistUpdateRequest.AliasesRequest(
                List.of(), List.of(), List.of());
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest(null, null, aliases);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistAliasRepository.findByArtistId(1L)).willReturn(List.of(existing));

        // when
        adminService.updateArtist(1L, request);

        // then
        ArgumentCaptor<List<ArtistAlias>> captor = ArgumentCaptor.forClass(List.class);
        verify(artistAliasRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        verify(artistAliasRepository, never()).saveAll(any());
    }

    @Test
    void should_not_touch_aliases_when_aliases_field_is_null() {
        // given
        Artist artist = Artist.builder().mbid("mbid-1").name("YOASOBI").isComing(false).build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest("요아소비", null, null);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        verify(artistAliasRepository, never()).findByArtistId(any());
        verify(artistAliasRepository, never()).deleteAll(any(List.class));
        verify(artistAliasRepository, never()).saveAll(any());
    }

    // -------------------------------------------------------------------------
    // getAdminArtist
    // -------------------------------------------------------------------------

    @Test
    void should_return_artist_with_grouped_aliases_when_valid_id_given() {
        // given
        Artist artist = Artist.builder().mbid("mbid-1").name("YOASOBI").isComing(true).build();
        ReflectionTestUtils.setField(artist, "id", ARTIST_ID);
        List<ArtistAlias> aliases = List.of(
                ArtistAlias.builder().artistId(ARTIST_ID).name("ヨアソビ").locale("ja").build(),
                ArtistAlias.builder().artistId(ARTIST_ID).name("よあそび").locale("ja").build(),
                ArtistAlias.builder().artistId(ARTIST_ID).name("YOASOBI").locale("en").build()
        );
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(artistAliasRepository.findByArtistId(ARTIST_ID)).willReturn(aliases);

        // when
        AdminArtistDetailResponse response = adminService.getAdminArtist(ARTIST_ID);

        // then
        assertThat(response.id()).isEqualTo(ARTIST_ID);
        assertThat(response.name()).isEqualTo("YOASOBI");
        assertThat(response.aliases().ja()).containsExactlyInAnyOrder("ヨアソビ", "よあそび");
        assertThat(response.aliases().en()).containsExactly("YOASOBI");
        assertThat(response.aliases().ko()).isEmpty();
    }

    @Test
    void should_return_empty_alias_lists_when_no_aliases_exist() {
        // given
        Artist artist = Artist.builder().mbid("mbid-1").name("IU").isComing(true).build();
        ReflectionTestUtils.setField(artist, "id", ARTIST_ID);
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(artistAliasRepository.findByArtistId(ARTIST_ID)).willReturn(List.of());

        // when
        AdminArtistDetailResponse response = adminService.getAdminArtist(ARTIST_ID);

        // then
        assertThat(response.aliases().ja()).isEmpty();
        assertThat(response.aliases().en()).isEmpty();
        assertThat(response.aliases().ko()).isEmpty();
    }

    @Test
    void should_throw_artist_not_found_when_get_admin_artist_with_invalid_id() {
        // given
        given(artistRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.getAdminArtist(999L))
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
    // getAdminConcert
    // -------------------------------------------------------------------------

    @Test
    void should_return_image_urls_ordered_by_position_when_valid_id_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        List<ConcertImage> images = List.of(
                ConcertImage.builder().id(1L).concertId(CONCERT_ID).url("https://example.com/1.jpg").position(0).build(),
                ConcertImage.builder().id(2L).concertId(CONCERT_ID).url("https://example.com/2.jpg").position(1).build()
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(artistRepository.findAllById(List.of())).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(concertImageRepository.findByConcertIdOrderByPosition(CONCERT_ID)).willReturn(images);

        // when
        AdminConcertDetailResponse response = adminService.getAdminConcert(CONCERT_ID);

        // then
        assertThat(response.imageUrls()).containsExactly("https://example.com/1.jpg", "https://example.com/2.jpg");
    }

    @Test
    void should_throw_concert_not_found_when_get_admin_concert_with_invalid_id() {
        // given
        given(concertRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.getAdminConcert(999L))
                .isInstanceOf(ConcertNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // createConcert
    // -------------------------------------------------------------------------

    private void givenSaveReturnsConcertWithId(Long id) {
        given(concertRepository.save(any(Concert.class))).willAnswer(invocation -> {
            Concert argument = invocation.getArgument(0);
            return Concert.builder()
                    .id(id)
                    .kopisId(argument.getKopisId())
                    .title(argument.getTitle())
                    .cast(argument.getCast())
                    .startDate(argument.getStartDate())
                    .endDate(argument.getEndDate())
                    .venueName(argument.getVenueName())
                    .posterUrl(argument.getPosterUrl())
                    .price(argument.getPrice())
                    .status(argument.getStatus())
                    .viewCount(argument.getViewCount())
                    .kopisUpdateDate(argument.getKopisUpdateDate())
                    .ticketOpenAt(argument.getTicketOpenAt())
                    .build();
        });
    }

    @Test
    void should_return_saved_concert_id_when_valid_request_given() {
        // given
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", "아이유",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "올림픽공원 체조경기장", "https://example.com/poster.jpg",
                null, "VIP 150,000원", null, null
        );
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        AdminConcertCreateResponse response = adminService.createConcert(request);

        // then
        verify(concertRepository).save(any(Concert.class));
        assertThat(response.concertId()).isEqualTo(CONCERT_ID);
    }

    @Test
    void should_save_concert_when_no_duplicate_exists() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", "아이유",
                startDate, endDate,
                "올림픽공원 체조경기장", null,
                null, null, null, null
        );
        given(concertRepository.existsByTitleAndStartDateAndEndDate("아이유 콘서트", startDate, endDate))
                .willReturn(false);
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        AdminConcertCreateResponse response = adminService.createConcert(request);

        // then
        verify(concertRepository).save(any(Concert.class));
        assertThat(response.concertId()).isEqualTo(CONCERT_ID);
    }

    @Test
    void should_throw_concert_already_exists_when_duplicate_title_and_period_given() {
        // given
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", "아이유",
                startDate, endDate,
                "올림픽공원 체조경기장", null,
                null, null, null, null
        );
        given(concertRepository.existsByTitleAndStartDateAndEndDate("아이유 콘서트", startDate, endDate))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminService.createConcert(request))
                .isInstanceOf(ConcertAlreadyExistsException.class);
        verify(concertRepository, never()).save(any());
    }

    @Test
    void should_set_status_upcoming_when_start_date_is_in_the_future() {
        // given
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "올림픽공원 체조경기장", null,
                null, null, null, null
        );
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        adminService.createConcert(request);

        // then
        ArgumentCaptor<Concert> captor = ArgumentCaptor.forClass(Concert.class);
        verify(concertRepository).save(captor.capture());
        Concert saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ConcertStatus.UPCOMING);
        assertThat(saved.getViewCount()).isEqualTo(0L);
        assertThat(saved.getKopisId()).isNull();
    }

    @Test
    void should_save_booking_links_when_booking_links_given() {
        // given
        List<BookingLinkRequest> links = List.of(
                new BookingLinkRequest("인터파크", "https://interpark.com/ticket/1")
        );
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "올림픽공원 체조경기장", null,
                null, null, null, links
        );
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        adminService.createConcert(request);

        // then
        verify(concertBookingLinkRepository).deleteByConcertId(CONCERT_ID);
        ArgumentCaptor<List<ConcertBookingLink>> captor = ArgumentCaptor.forClass(List.class);
        verify(concertBookingLinkRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getName()).isEqualTo("인터파크");
    }

    @Test
    void should_not_touch_booking_links_when_booking_links_is_null_on_create() {
        // given
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "올림픽공원 체조경기장", null,
                null, null, null, null
        );
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        adminService.createConcert(request);

        // then
        verify(concertBookingLinkRepository, never()).deleteByConcertId(any());
        verify(concertBookingLinkRepository, never()).saveAll(any());
    }

    @Test
    void should_save_concert_images_when_image_urls_given() {
        // given
        List<String> imageUrls = List.of(
                "https://example.com/1.jpg",
                "https://example.com/2.jpg"
        );
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "올림픽공원 체조경기장", null,
                imageUrls, null, null, null
        );
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        adminService.createConcert(request);

        // then
        verify(concertImageRepository).deleteByConcertId(CONCERT_ID);
        ArgumentCaptor<List<ConcertImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(concertImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getPosition()).isEqualTo(0);
        assertThat(captor.getValue().get(1).getPosition()).isEqualTo(1);
    }

    @Test
    void should_not_touch_concert_images_when_image_urls_is_null_on_create() {
        // given
        AdminConcertCreateRequest request = new AdminConcertCreateRequest(
                "아이유 콘서트", null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "올림픽공원 체조경기장", null,
                null, null, null, null
        );
        givenSaveReturnsConcertWithId(CONCERT_ID);

        // when
        adminService.createConcert(request);

        // then
        verify(concertImageRepository, never()).deleteByConcertId(any());
        verify(concertImageRepository, never()).saveAll(any());
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
                null, null, null, "R석 100,000원", null, null
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
                null, null, null, null, null, null, null, null, null, links
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
                "새 제목", null, null, null, null, null, null, null, null, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        verify(concertBookingLinkRepository, never()).deleteByConcertId(any());
        verify(concertBookingLinkRepository, never()).saveAll(any());
    }

    @Test
    void should_replace_concert_images_when_image_urls_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        List<String> imageUrls = List.of(
                "https://example.com/1.jpg",
                "https://example.com/2.jpg"
        );
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                null, null, null, null, null, null, imageUrls, null, null, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        verify(concertImageRepository).deleteByConcertId(CONCERT_ID);
        ArgumentCaptor<List<ConcertImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(concertImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getUrl()).isEqualTo("https://example.com/1.jpg");
        assertThat(captor.getValue().get(0).getPosition()).isEqualTo(0);
        assertThat(captor.getValue().get(1).getUrl()).isEqualTo("https://example.com/2.jpg");
        assertThat(captor.getValue().get(1).getPosition()).isEqualTo(1);
    }

    @Test
    void should_not_touch_concert_images_when_image_urls_is_null() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                "새 제목", null, null, null, null, null, null, null, null, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        verify(concertImageRepository, never()).deleteByConcertId(any());
        verify(concertImageRepository, never()).saveAll(any());
    }

    @Test
    void should_delete_all_concert_images_when_image_urls_is_empty_list() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                null, null, null, null, null, null, List.of(), null, null, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        verify(concertImageRepository).deleteByConcertId(CONCERT_ID);
        ArgumentCaptor<List<ConcertImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(concertImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void should_update_ticket_open_at_when_ticket_open_at_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        LocalDateTime ticketOpenAt = LocalDateTime.of(2025, 8, 1, 10, 0);
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                null, null, null, null, null, null, null, null, ticketOpenAt, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        assertThat(concert.getTicketOpenAt()).isEqualTo(ticketOpenAt);
    }

    @Test
    void should_clear_ticket_open_at_when_null_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        ReflectionTestUtils.setField(concert, "ticketOpenAt", LocalDateTime.of(2025, 8, 1, 10, 0));
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                null, null, null, null, null, null, null, null, null, null
        );
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.updateConcert(CONCERT_ID, request);

        // then
        assertThat(concert.getTicketOpenAt()).isNull();
    }

    @Test
    void should_throw_concert_not_found_when_update_target_does_not_exist() {
        // given
        AdminConcertUpdateRequest request = new AdminConcertUpdateRequest(
                "새 제목", null, null, null, null, null, null, null, null, null
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
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).build();

        given(concertRepository.findByStatus(ConcertStatus.PENDING, PAGEABLE)).willReturn(page);
        given(concertArtistCandidateRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertBookingLinkRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        PageResponse<AdminPendingConcertResponse> response = adminService.getPendingConcerts(PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(CONCERT_ID);
        assertThat(response.content().get(0).candidates()).hasSize(1);
        assertThat(response.content().get(0).candidates().get(0).artistId()).isEqualTo(ARTIST_ID);
        assertThat(response.content().get(0).candidates().get(0).name()).isEqualTo("YOASOBI");
        assertThat(response.content().get(0).bookingLinks()).isEmpty();
    }

    @Test
    void should_return_booking_links_when_pending_concert_has_booking_links() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        ConcertBookingLink link = ConcertBookingLink.builder()
                .concertId(CONCERT_ID).name("인터파크").url("https://interpark.com").build();

        given(concertRepository.findByStatus(ConcertStatus.PENDING, PAGEABLE)).willReturn(page);
        given(concertArtistCandidateRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());
        given(artistRepository.findAllById(List.of())).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of(link));

        // when
        PageResponse<AdminPendingConcertResponse> response = adminService.getPendingConcerts(PAGEABLE);

        // then
        assertThat(response.content().get(0).bookingLinks()).hasSize(1);
        assertThat(response.content().get(0).bookingLinks().get(0).name()).isEqualTo("인터파크");
        assertThat(response.content().get(0).bookingLinks().get(0).url()).isEqualTo("https://interpark.com");
    }

    @Test
    void should_return_empty_candidate_list_when_concert_has_no_candidates() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByStatus(ConcertStatus.PENDING, PAGEABLE)).willReturn(page);
        given(concertArtistCandidateRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());
        given(artistRepository.findAllById(List.of())).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        PageResponse<AdminPendingConcertResponse> response = adminService.getPendingConcerts(PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).candidates()).isEmpty();
        assertThat(response.content().get(0).bookingLinks()).isEmpty();
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
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.approveConcert(CONCERT_ID, null);

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
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(false);

        // when
        adminService.approveConcert(CONCERT_ID, null);

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
        assertThatThrownBy(() -> adminService.approveConcert(CONCERT_ID, null))
                .isInstanceOf(ConcertNotPendingException.class);
    }

    @Test
    void should_set_ticket_open_at_when_approve_request_has_ticket_open_at() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        ReflectionTestUtils.setField(concert, "startDate", LocalDate.now().plusDays(10));
        ReflectionTestUtils.setField(concert, "endDate", LocalDate.now().plusDays(12));
        LocalDateTime ticketOpenAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        AdminConcertApproveRequest request = new AdminConcertApproveRequest(ticketOpenAt, null);
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.approveConcert(CONCERT_ID, request);

        // then
        assertThat(concert.getTicketOpenAt()).isEqualTo(ticketOpenAt);
    }

    @Test
    void should_save_booking_links_when_approve_request_has_booking_links() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        ReflectionTestUtils.setField(concert, "startDate", LocalDate.now().plusDays(10));
        ReflectionTestUtils.setField(concert, "endDate", LocalDate.now().plusDays(12));
        AdminConcertApproveRequest request = new AdminConcertApproveRequest(
                null,
                List.of(new BookingLinkRequest("인터파크", "https://interpark.com"))
        );
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.approveConcert(CONCERT_ID, request);

        // then
        verify(concertBookingLinkRepository).saveAll(any());
    }

    @Test
    void should_not_save_booking_links_when_approve_request_is_null() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        ReflectionTestUtils.setField(concert, "startDate", LocalDate.now().plusDays(10));
        ReflectionTestUtils.setField(concert, "endDate", LocalDate.now().plusDays(12));
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .id(1L).concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("YOASOBI").isComing(false).build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistCandidateRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(candidate));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.approveConcert(CONCERT_ID, null);

        // then
        verify(concertBookingLinkRepository, never()).saveAll(any());
        assertThat(concert.getTicketOpenAt()).isNull();
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
    // assignCandidateToConcert
    // -------------------------------------------------------------------------

    @Test
    void should_save_candidate_when_concert_is_pending() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertArtistCandidateRepository.existsByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID)).willReturn(false);

        // when
        adminService.assignCandidateToConcert(CONCERT_ID, ARTIST_ID);

        // then
        verify(concertArtistCandidateRepository).save(any());
    }

    @Test
    void should_throw_concert_not_pending_when_assign_candidate_to_non_pending_concert() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> adminService.assignCandidateToConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertNotPendingException.class);
        verify(concertArtistCandidateRepository, never()).save(any());
    }

    @Test
    void should_throw_artist_not_found_when_assign_candidate_with_invalid_artist() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.existsById(ARTIST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.assignCandidateToConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ArtistNotFoundException.class);
        verify(concertArtistCandidateRepository, never()).save(any());
    }

    @Test
    void should_throw_concert_artist_already_exists_when_candidate_already_mapped() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertArtistCandidateRepository.existsByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminService.assignCandidateToConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertArtistAlreadyExistsException.class);
        verify(concertArtistCandidateRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // removeCandidateFromConcert
    // -------------------------------------------------------------------------

    @Test
    void should_delete_candidate_when_concert_is_pending() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        ConcertArtistCandidate candidate = ConcertArtistCandidate.builder()
                .concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertArtistCandidateRepository.findByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID))
                .willReturn(Optional.of(candidate));

        // when
        adminService.removeCandidateFromConcert(CONCERT_ID, ARTIST_ID);

        // then
        verify(concertArtistCandidateRepository).delete(candidate);
    }

    @Test
    void should_throw_concert_not_pending_when_remove_candidate_from_non_pending_concert() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> adminService.removeCandidateFromConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertNotPendingException.class);
        verify(concertArtistCandidateRepository, never()).delete(any());
    }

    @Test
    void should_throw_artist_not_found_when_remove_candidate_with_invalid_artist() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.existsById(ARTIST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.removeCandidateFromConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ArtistNotFoundException.class);
        verify(concertArtistCandidateRepository, never()).delete(any());
    }

    @Test
    void should_throw_concert_artist_not_found_when_candidate_not_mapped() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertArtistCandidateRepository.findByConcertIdAndArtistId(CONCERT_ID, ARTIST_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.removeCandidateFromConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertArtistNotFoundException.class);
        verify(concertArtistCandidateRepository, never()).delete(any());
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

    @Test
    void should_throw_concert_is_pending_when_assign_artist_to_pending_concert() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> adminService.assignArtistToConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertIsPendingException.class);
        verify(concertArtistRepository, never()).save(any());
    }

    @Test
    void should_throw_concert_is_pending_when_remove_artist_from_pending_concert() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.PENDING);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when & then
        assertThatThrownBy(() -> adminService.removeArtistFromConcert(CONCERT_ID, ARTIST_ID))
                .isInstanceOf(ConcertIsPendingException.class);
        verify(concertArtistRepository, never()).delete(any());
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
        PipelineArtistCollectResult result = new PipelineArtistCollectResult(true, 1L, "some-mbid-123", "IU", null, List.of(), null);
        given(dataPipelineClient.collectArtist("some-mbid-123")).willReturn(result);

        // when
        PipelineArtistCollectResult actual = adminService.collectArtist(request);

        // then
        verify(dataPipelineClient).collectArtist("some-mbid-123");
        assertThat(actual.success()).isTrue();
    }

    // -------------------------------------------------------------------------
    // collectConcert
    // -------------------------------------------------------------------------

    @Test
    void should_trigger_concert_collect_when_kopis_id_given() {
        // given
        AdminConcertCollectRequest request = new AdminConcertCollectRequest("PF123456");
        PipelineConcertCollectResult result = new PipelineConcertCollectResult(true, 10L, "YOASOBI Live", List.of(), null);
        given(dataPipelineClient.collectConcert("PF123456")).willReturn(result);

        // when
        PipelineConcertCollectResult actual = adminService.collectConcert(request);

        // then
        verify(dataPipelineClient).collectConcert("PF123456");
        assertThat(actual.success()).isTrue();
    }

    // -------------------------------------------------------------------------
    // forceChangeConcertState — is_coming 갱신 케이스 추가
    // -------------------------------------------------------------------------

    @Test
    void should_update_artist_is_coming_when_status_changed_to_upcoming() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.EXCLUDED);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("IU").isComing(false).build();
        ConcertArtist concertArtist = ConcertArtist.builder().concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        AdminConcertStateUpdateRequest request = new AdminConcertStateUpdateRequest(ConcertStatus.UPCOMING, null);

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.forceChangeConcertState(CONCERT_ID, request);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.UPCOMING);
        assertThat(artist.isComing()).isTrue();
        verify(concertArtistRepository).findByConcertId(CONCERT_ID);
    }

    @Test
    void should_update_artist_is_coming_when_status_changed_to_ongoing() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.EXCLUDED);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("IU").isComing(false).build();
        ConcertArtist concertArtist = ConcertArtist.builder().concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        AdminConcertStateUpdateRequest request = new AdminConcertStateUpdateRequest(ConcertStatus.ONGOING, null);

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));
        given(concertRepository.existsActiveByArtistId(eq(ARTIST_ID), any())).willReturn(true);

        // when
        adminService.forceChangeConcertState(CONCERT_ID, request);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.ONGOING);
        assertThat(artist.isComing()).isTrue();
        verify(concertArtistRepository).findByConcertId(CONCERT_ID);
    }

    @Test
    void should_not_query_concert_artist_when_status_changed_to_inactive_state() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        AdminConcertStateUpdateRequest request = new AdminConcertStateUpdateRequest(ConcertStatus.CANCELLED, null);

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));

        // when
        adminService.forceChangeConcertState(CONCERT_ID, request);

        // then
        assertThat(concert.getStatus()).isEqualTo(ConcertStatus.CANCELLED);
        verify(concertArtistRepository, never()).findByConcertId(any());
    }

    // -------------------------------------------------------------------------
    // getExcludedConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_excluded_concerts_with_artist_info_when_concert_artist_exists() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.EXCLUDED);
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("IU").isComing(false).build();
        ConcertArtist concertArtist = ConcertArtist.builder().concertId(CONCERT_ID).artistId(ARTIST_ID).build();
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByStatus(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<AdminExcludedConcertResponse> response = adminService.getExcludedConcerts(PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(CONCERT_ID);
        assertThat(response.content().get(0).artists()).hasSize(1);
        assertThat(response.content().get(0).artists().get(0).artistId()).isEqualTo(ARTIST_ID);
        assertThat(response.content().get(0).artists().get(0).name()).isEqualTo("IU");
    }

    @Test
    void should_return_empty_artist_list_when_excluded_concert_has_no_concert_artist() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.EXCLUDED);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByStatus(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());
        given(artistRepository.findAllById(List.of())).willReturn(List.of());

        // when
        PageResponse<AdminExcludedConcertResponse> response = adminService.getExcludedConcerts(PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(CONCERT_ID);
        assertThat(response.content().get(0).artists()).isEmpty();
    }

    @Test
    void should_return_empty_page_when_no_excluded_concerts_exist() {
        // given
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findByStatus(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(emptyPage);

        // when
        PageResponse<AdminExcludedConcertResponse> response = adminService.getExcludedConcerts(PAGEABLE);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    // -------------------------------------------------------------------------
    // searchLocalArtists
    // -------------------------------------------------------------------------

    @Test
    void should_return_artist_id_and_name_when_matching_artists_found() {
        // given
        Artist artist = Artist.builder().id(ARTIST_ID).mbid("mbid-1").name("IU").isComing(true).build();
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase("IU", PAGEABLE)).willReturn(page);

        // when
        PageResponse<AdminArtistSearchResult> response = adminService.searchLocalArtists("IU", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(ARTIST_ID);
        assertThat(response.content().get(0).name()).isEqualTo("IU");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void should_return_empty_page_when_no_local_artists_match() {
        // given
        Page<Artist> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase("존재하지않는아티스트", PAGEABLE)).willReturn(emptyPage);

        // when
        PageResponse<AdminArtistSearchResult> response = adminService.searchLocalArtists("존재하지않는아티스트", PAGEABLE);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }
}
