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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ArtistRepository artistRepository;
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ConcertBookingLinkRepository concertBookingLinkRepository;
    private final ConcertStatusLogRepository concertStatusLogRepository;
    private final SetlistRepository setlistRepository;
    private final SetlistTrackRepository setlistTrackRepository;
    private final UserConcertCalendarRepository userConcertCalendarRepository;

    /**
     * 아티스트를 수동 등록한다.
     */
    @Transactional
    public void createArtist(AdminArtistCreateRequest request) {
        artistRepository.save(Artist.builder()
                .mbid(request.mbid())
                .name(request.name())
                .sortName(request.sortName())
                .isComing(false)
                .build());
    }

    /**
     * 아티스트 정보를 수정한다. 존재하지 않는 아티스트 ID이면 ArtistNotFoundException을 던진다.
     */
    @Transactional
    public void updateArtist(Long id, AdminArtistUpdateRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(ArtistNotFoundException::new);
        artist.update(request.name(), request.sortName());
    }

    /**
     * 전체 문의 목록을 조회한다. type·status 중 null인 항목은 필터 없이 조회한다.
     */
    public PageResponse<AdminInquiryListItemResponse> getInquiries(InquiryType type, InquiryStatus status, Pageable pageable) {
        Page<Inquiry> page;
        if (type != null && status != null) {
            page = inquiryRepository.findAllByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            page = inquiryRepository.findAllByType(type, pageable);
        } else if (status != null) {
            page = inquiryRepository.findAllByStatus(status, pageable);
        } else {
            page = inquiryRepository.findAll(pageable);
        }

        List<Long> userIds = page.getContent().stream()
                .map(Inquiry::getUserId)
                .distinct()
                .toList();
        Map<Long, String> nicknameByUserId = userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return PageResponse.from(page.map(inquiry ->
                AdminInquiryListItemResponse.of(inquiry, nicknameByUserId.getOrDefault(inquiry.getUserId(), ""))));
    }

    /**
     * 문의 상세를 조회한다. 존재하지 않는 ID이면 InquiryNotFoundException을 던진다.
     */
    public AdminInquiryDetailResponse getInquiryDetail(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(InquiryNotFoundException::new);
        String nickname = userRepository.findById(inquiry.getUserId())
                .map(User::getNickname)
                .orElse("");
        return AdminInquiryDetailResponse.of(inquiry, nickname);
    }

    /**
     * 문의 처리 상태를 변경한다. 존재하지 않는 ID이면 InquiryNotFoundException을 던진다.
     */
    @Transactional
    public void updateInquiryStatus(Long id, AdminInquiryStatusUpdateRequest request) {
        if (request.status() == InquiryStatus.PENDING) {
            throw new InvalidInquiryStatusException();
        }
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(InquiryNotFoundException::new);
        inquiry.updateStatus(request.status(), request.adminNote());
    }

    /**
     * 공연을 수동 등록한다. artistIds가 있으면 ConcertArtist 매핑도 함께 저장한다.
     */
    @Transactional
    public void createConcert(AdminConcertCreateRequest request) {
        Concert concert = concertRepository.save(Concert.builder()
                .kopisId(request.kopisId())
                .title(request.title())
                .cast(request.cast())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .venueName(request.venueName())
                .venueAddress(request.venueAddress())
                .posterUrl(request.posterUrl())
                .price(request.price())
                .status(request.status())
                .viewCount(0L)
                .kopisUpdateDate(request.startDate())
                .build());

        if (request.artistIds() != null) {
            List<ConcertArtist> mappings = request.artistIds().stream()
                    .map(artistId -> ConcertArtist.builder()
                            .concertId(concert.getId())
                            .artistId(artistId)
                            .confidence("HIGH")
                            .matchedBy("ADMIN")
                            .build())
                    .toList();
            concertArtistRepository.saveAll(mappings);
        }
    }

    /**
     * 공연 정보를 수정한다. bookingLinks가 있으면 기존 링크를 삭제 후 새로 저장한다. 존재하지 않는 ID이면 ConcertNotFoundException을 던진다.
     */
    @Transactional
    public void updateConcert(Long id, AdminConcertUpdateRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);
        concert.update(request.title(), request.cast(), request.startDate(), request.endDate(),
                request.venueName(), request.venueAddress(), request.posterUrl(), request.price());

        if (request.bookingLinks() != null) {
            concertBookingLinkRepository.deleteByConcertId(id);
            List<ConcertBookingLink> links = request.bookingLinks().stream()
                    .map(link -> ConcertBookingLink.builder()
                            .concertId(id)
                            .name(link.name())
                            .url(link.url())
                            .build())
                    .toList();
            concertBookingLinkRepository.saveAll(links);
        }
    }

    /**
     * 공연을 영구 삭제한다. 연관 데이터(상태 로그·셋리스트 트랙·셋리스트·유저 캘린더·아티스트 매핑·예매처 링크)를 순서대로 cascade 삭제한다. 존재하지 않는 ID이면 ConcertNotFoundException을 던진다.
     */
    @Transactional
    public void deleteConcert(Long id) {
        if (!concertRepository.existsById(id)) {
            throw new ConcertNotFoundException();
        }

        concertStatusLogRepository.deleteByConcertId(id);

        List<Long> setlistIds = setlistRepository.findByConcertId(id).stream()
                .map(Setlist::getId)
                .toList();
        if (!setlistIds.isEmpty()) {
            setlistTrackRepository.deleteBySetlistIdIn(setlistIds);
        }
        setlistRepository.deleteAllById(setlistIds);

        userConcertCalendarRepository.deleteByConcertId(id);
        concertArtistRepository.deleteByConcertId(id);
        concertBookingLinkRepository.deleteByConcertId(id);
        concertRepository.deleteById(id);
    }

    /**
     * 공연 상태를 강제 변경하고 변경 이력을 concert_status_log에 기록한다. 존재하지 않는 ID이면 ConcertNotFoundException을 던진다.
     */
    @Transactional
    public void forceChangeConcertState(Long id, AdminConcertStateUpdateRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);
        ConcertStatus previousStatus = concert.forceChangeStatus(request.status());
        concertStatusLogRepository.save(ConcertStatusLog.builder()
                .concertId(id)
                .beforeStatus(previousStatus)
                .afterStatus(request.status())
                .reason(request.reason())
                .changedAt(LocalDateTime.now())
                .build());
    }
}
