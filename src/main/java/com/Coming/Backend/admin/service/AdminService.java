package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.client.DataPipelineClient;
import com.Coming.Backend.admin.dto.AdminArtistCollectRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCollectRequest;
import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import com.Coming.Backend.admin.dto.AdminCandidateArtistResponse;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.AdminPendingConcertResponse;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.auth.entity.User;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ConcertArtistCandidateRepository concertArtistCandidateRepository;
    private final ConcertBookingLinkRepository concertBookingLinkRepository;
    private final DataPipelineClient dataPipelineClient;

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
     * 공연 상태를 강제 변경한다. 존재하지 않는 ID이면 ConcertNotFoundException을 던진다.
     */
    @Transactional
    public void forceChangeConcertState(Long id, AdminConcertStateUpdateRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);
        concert.forceChangeStatus(request.status());
    }

    /**
     * PENDING 상태 공연 목록과 각 공연의 후보 아티스트를 반환한다.
     */
    public PageResponse<AdminPendingConcertResponse> getPendingConcerts(Pageable pageable) {
        Page<Concert> page = concertRepository.findByStatus(ConcertStatus.PENDING, pageable);

        List<Long> concertIds = page.getContent().stream().map(Concert::getId).toList();
        Map<Long, List<ConcertArtistCandidate>> candidatesByConcertId = concertArtistCandidateRepository
                .findByConcertIdIn(concertIds).stream()
                .collect(Collectors.groupingBy(ConcertArtistCandidate::getConcertId));

        List<Long> artistIds = candidatesByConcertId.values().stream()
                .flatMap(List::stream)
                .map(ConcertArtistCandidate::getArtistId)
                .distinct()
                .toList();
        Map<Long, String> artistNameById = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Artist::getName));

        List<AdminPendingConcertResponse> content = page.getContent().stream()
                .map(concert -> {
                    List<AdminCandidateArtistResponse> candidates = candidatesByConcertId
                            .getOrDefault(concert.getId(), List.of()).stream()
                            .map(c -> new AdminCandidateArtistResponse(
                                    c.getArtistId(),
                                    artistNameById.get(c.getArtistId()),
                                    c.getMatchedBy()))
                            .toList();
                    return AdminPendingConcertResponse.of(concert, candidates);
                })
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * PENDING 공연을 승인한다. 후보 아티스트를 concert_artist로 이동하고, 날짜 기반으로 상태를 계산하며, 연결된 아티스트의 is_coming을 갱신한다.
     *
     * @throws ConcertNotFoundException   존재하지 않는 공연 ID
     * @throws ConcertNotPendingException 공연이 PENDING 상태가 아닌 경우
     */
    @Transactional
    public void approveConcert(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        if (concert.getStatus() != ConcertStatus.PENDING) {
            throw new ConcertNotPendingException();
        }

        List<ConcertArtistCandidate> candidates = concertArtistCandidateRepository.findByConcertId(concertId);

        List<ConcertArtist> mappings = candidates.stream()
                .map(c -> ConcertArtist.builder()
                        .concertId(c.getConcertId())
                        .artistId(c.getArtistId())
                        .build())
                .toList();
        concertArtistRepository.saveAll(mappings);
        concertArtistCandidateRepository.deleteByConcertId(concertId);

        ConcertStatus computedStatus = computeStatusFromDates(concert.getStartDate(), concert.getEndDate());
        concert.forceChangeStatus(computedStatus);

        List<Long> artistIds = candidates.stream().map(ConcertArtistCandidate::getArtistId).distinct().toList();
        List<ConcertStatus> activeStatuses = List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING);
        artistRepository.findAllById(artistIds).forEach(artist ->
                artist.updateIsComing(concertRepository.existsActiveByArtistId(artist.getId(), activeStatuses)));
    }

    /**
     * PENDING 공연을 거절한다. 상태를 EXCLUDED로 변경하고 후보 아티스트와 확정 매핑 목록을 삭제한다.
     *
     * @throws ConcertNotFoundException   존재하지 않는 공연 ID
     * @throws ConcertNotPendingException 공연이 PENDING 상태가 아닌 경우
     */
    @Transactional
    public void rejectConcert(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        if (concert.getStatus() != ConcertStatus.PENDING) {
            throw new ConcertNotPendingException();
        }
        concert.forceChangeStatus(ConcertStatus.EXCLUDED);
        concertArtistCandidateRepository.deleteByConcertId(concertId);
        concertArtistRepository.deleteByConcertId(concertId);
    }

    /**
     * 공연에 아티스트를 직접 매핑한다. concert_artist_candidate를 거치지 않고 concert_artist에 바로 저장한다.
     * 공연 상태가 UPCOMING 또는 ONGOING이면 해당 아티스트의 is_coming을 true로 갱신한다.
     *
     * @throws ConcertNotFoundException            존재하지 않는 공연 ID
     * @throws ArtistNotFoundException             존재하지 않는 아티스트 ID
     * @throws ConcertArtistAlreadyExistsException 이미 매핑된 아티스트
     */
    @Transactional
    public void assignArtistToConcert(Long concertId, Long artistId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);
        if (concertArtistRepository.existsByConcertIdAndArtistId(concertId, artistId)) {
            throw new ConcertArtistAlreadyExistsException();
        }
        concertArtistRepository.save(ConcertArtist.builder()
                .concertId(concertId)
                .artistId(artistId)
                .build());

        if (concert.getStatus() == ConcertStatus.UPCOMING || concert.getStatus() == ConcertStatus.ONGOING) {
            artist.updateIsComing(true);
        }
    }

    /**
     * MusicBrainz에서 아티스트명으로 후보를 검색한다.
     */
    public List<DataArtistSearchResult> searchArtists(String name) {
        return dataPipelineClient.searchArtists(name);
    }

    /**
     * KOPIS에서 공연명으로 후보를 검색한다.
     */
    public List<DataConcertSearchResult> searchConcerts(String title) {
        return dataPipelineClient.searchConcerts(title);
    }

    /**
     * MBID 기반으로 Data 파이프라인에 아티스트 초기 수집을 트리거한다.
     */
    public void collectArtist(AdminArtistCollectRequest request) {
        dataPipelineClient.triggerArtistCollect(request.mbid());
    }

    /**
     * KOPIS ID 기반으로 Data 파이프라인에 공연 수집을 트리거한다.
     */
    public void collectConcert(AdminConcertCollectRequest request) {
        dataPipelineClient.triggerConcertCollectByKopisId(request.kopisId());
    }

    /**
     * Data 파이프라인에 특정 아티스트의 릴리즈 수집을 트리거한다.
     */
    public void triggerArtistReleases(Long artistId) {
        dataPipelineClient.triggerArtistReleases(artistId);
    }

    /**
     * Data 파이프라인에 특정 공연의 셋리스트 수집을 트리거한다.
     */
    public void triggerConcertSetlist(Long concertId) {
        dataPipelineClient.triggerConcertSetlist(concertId);
    }

    private ConcertStatus computeStatusFromDates(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) {
            return ConcertStatus.UPCOMING;
        }
        if (!today.isAfter(endDate)) {
            return ConcertStatus.ONGOING;
        }
        return ConcertStatus.ENDED;
    }
}
