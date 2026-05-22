package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.dto.AdminArtistCreateRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
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
                "IU",
                LocalDate.of(2008, 9, 18)
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
        assertThat(saved.getDebutDate()).isEqualTo(LocalDate.of(2008, 9, 18));
        assertThat(saved.isComing()).isFalse();
    }

    @Test
    void should_update_all_fields_when_full_update_request_given() {
        // given
        Artist artist = Artist.builder()
                .mbid("some-mbid-123")
                .name("IU")
                .sortName("IU")
                .debutDate(LocalDate.of(2008, 9, 18))
                .isComing(false)
                .build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest(
                "아이유",
                "Iu, Lee Ji Eun",
                LocalDate.of(2008, 9, 18)
        );
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        assertThat(artist.getName()).isEqualTo("아이유");
        assertThat(artist.getSortName()).isEqualTo("Iu, Lee Ji Eun");
        assertThat(artist.getDebutDate()).isEqualTo(LocalDate.of(2008, 9, 18));
    }

    @Test
    void should_not_overwrite_null_fields_when_partial_update_request_given() {
        // given
        Artist artist = Artist.builder()
                .mbid("some-mbid-123")
                .name("IU")
                .sortName("IU")
                .debutDate(LocalDate.of(2008, 9, 18))
                .isComing(false)
                .build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest("아이유", null, null);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        assertThat(artist.getName()).isEqualTo("아이유");
        assertThat(artist.getSortName()).isEqualTo("IU");
        assertThat(artist.getDebutDate()).isEqualTo(LocalDate.of(2008, 9, 18));
    }

    @Test
    void should_throw_artist_not_found_exception_when_invalid_id_given() {
        // given
        given(artistRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateArtist(999L, new AdminArtistUpdateRequest("IU", null, null)))
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
}
