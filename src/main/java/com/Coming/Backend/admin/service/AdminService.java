package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.client.DataPipelineClient;
import com.Coming.Backend.admin.dto.AdminArtistCollectRequest;
import com.Coming.Backend.admin.dto.AdminArtistDetailResponse;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCollectRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateResponse;
import com.Coming.Backend.admin.dto.BookingLinkRequest;
import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import com.Coming.Backend.admin.dto.PipelineArtistCollectResult;
import com.Coming.Backend.admin.dto.PipelineConcertCollectResult;
import com.Coming.Backend.admin.dto.PipelineSetlistCollectResult;
import com.Coming.Backend.admin.dto.AdminCandidateArtistResponse;
import com.Coming.Backend.admin.dto.AdminConcertDetailResponse;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminArtistSearchResult;
import com.Coming.Backend.admin.dto.AdminExcludedArtistResponse;
import com.Coming.Backend.admin.dto.AdminExcludedConcertResponse;
import com.Coming.Backend.admin.dto.AdminConcertApproveRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.AdminPendingConcertResponse;
import com.Coming.Backend.admin.exception.PipelineConflictException;
import com.Coming.Backend.admin.repository.ArtistCollectLockRepository;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.entity.ArtistUrl;
import com.Coming.Backend.concert.dto.ArtistSummary;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.ArtistUrlRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertArtistCandidate;
import com.Coming.Backend.concert.entity.ConcertBookingLink;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertAlreadyExistsException;
import com.Coming.Backend.concert.exception.ConcertArtistAlreadyExistsException;
import com.Coming.Backend.concert.exception.ConcertArtistNotFoundException;
import com.Coming.Backend.concert.exception.ConcertIsPendingException;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.exception.ConcertNotPendingException;
import com.Coming.Backend.concert.entity.ConcertImage;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;
    private final ArtistUrlRepository artistUrlRepository;
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ConcertArtistCandidateRepository concertArtistCandidateRepository;
    private final ConcertBookingLinkRepository concertBookingLinkRepository;
    private final ConcertImageRepository concertImageRepository;
    private final DataPipelineClient dataPipelineClient;
    private final ArtistCollectLockRepository artistCollectLockRepository;

    /**
     * 어드민 아티스트 단건을 조회한다. 존재하지 않는 아티스트 ID이면 ArtistNotFoundException을 던진다.
     */
    public AdminArtistDetailResponse getAdminArtist(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(ArtistNotFoundException::new);
        return AdminArtistDetailResponse.of(artist, artistAliasRepository.findByArtistId(id),
                artistUrlRepository.findByArtistId(id));
    }

    /**
     * 아티스트 정보를 수정한다. aliases가 전달된 경우 기존 alias와 diff를 계산해 추가·삭제하고, links가 전달된 경우 기존 링크를 전체 교체한다.
     * imageUrl은 빈 문자열이면 삭제, null이면 무변경으로 처리한다. 존재하지 않는 아티스트 ID이면 ArtistNotFoundException을 던진다.
     */
    @Transactional
    public void updateArtist(Long id, AdminArtistUpdateRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(ArtistNotFoundException::new);
        artist.update(request.name(), request.sortName());

        if (request.imageUrl() != null) {
            artist.updateImageUrl(request.imageUrl().isBlank() ? null : request.imageUrl());
        }

        if (request.aliases() != null) {
            applyAliasesDiff(id, request.aliases());
        }

        if (request.links() != null) {
            replaceLinks(id, request.links());
        }
    }

    private void replaceLinks(Long artistId, List<AdminArtistUpdateRequest.LinkRequest> links) {
        Set<String> types = links.stream()
                .map(AdminArtistUpdateRequest.LinkRequest::type)
                .collect(Collectors.toSet());
        if (types.size() != links.size()) {
            throw new InvalidInputException();
        }

        artistUrlRepository.deleteByArtistId(artistId);
        if (links.isEmpty()) {
            return;
        }
        List<ArtistUrl> entities = links.stream()
                .map(link -> ArtistUrl.builder().artistId(artistId).type(link.type()).url(link.url()).build())
                .toList();
        artistUrlRepository.saveAll(entities);
    }

    private void applyAliasesDiff(Long artistId, AdminArtistUpdateRequest.AliasesRequest aliases) {
        Map<String, List<ArtistAlias>> existingByLocale = artistAliasRepository.findByArtistId(artistId)
                .stream()
                .filter(a -> a.getLocale() != null)
                .collect(Collectors.groupingBy(ArtistAlias::getLocale));

        List<ArtistAlias> toDelete = new ArrayList<>();
        List<ArtistAlias> toInsert = new ArrayList<>();

        collectLocaleDiff(artistId, "ja", aliases.ja(), existingByLocale.getOrDefault("ja", List.of()), toDelete, toInsert);
        collectLocaleDiff(artistId, "en", aliases.en(), existingByLocale.getOrDefault("en", List.of()), toDelete, toInsert);
        collectLocaleDiff(artistId, "ko", aliases.ko(), existingByLocale.getOrDefault("ko", List.of()), toDelete, toInsert);

        if (!toDelete.isEmpty()) artistAliasRepository.deleteAll(toDelete);
        if (!toInsert.isEmpty()) artistAliasRepository.saveAll(toInsert);
    }

    private void collectLocaleDiff(Long artistId, String locale, List<String> requested,
            List<ArtistAlias> existing, List<ArtistAlias> toDelete, List<ArtistAlias> toInsert) {
        if (requested == null) return;

        Set<String> requestedNames = requested.stream()
                .filter(n -> n != null && !n.isBlank())
                .collect(Collectors.toSet());
        Set<String> existingNames = existing.stream()
                .map(ArtistAlias::getName)
                .collect(Collectors.toSet());

        existing.stream()
                .filter(a -> !requestedNames.contains(a.getName()))
                .forEach(toDelete::add);

        requestedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> ArtistAlias.builder().artistId(artistId).name(name).locale(locale).build())
                .forEach(toInsert::add);
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
     * status 제한 없이 공연 단건을 조회한다. EXCLUDED 포함 모든 상태 조회 가능.
     *
     * @throws ConcertNotFoundException 존재하지 않는 공연 ID
     */
    public AdminConcertDetailResponse getAdminConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);
        List<Long> artistIds = concertArtistRepository.findByConcertId(id).stream()
                .map(ConcertArtist::getArtistId)
                .toList();
        Map<Long, String> koreanNameMap = buildKoreanNameMap(artistIds);
        List<ArtistSummary> artists = artistRepository.findAllById(artistIds).stream()
                .map(a -> new ArtistSummary(a.getId(), a.getName(), koreanNameMap.get(a.getId())))
                .toList();
        return AdminConcertDetailResponse.of(concert, concertBookingLinkRepository.findByConcertId(id), artists,
                buildImageUrls(id));
    }

    private List<String> buildImageUrls(Long concertId) {
        return concertImageRepository.findByConcertIdOrderByPosition(concertId).stream()
                .map(ConcertImage::getUrl)
                .toList();
    }

    /**
     * 공연 정보를 수정한다. bookingLinks·imageUrls가 있으면 각각 기존 목록을 삭제 후 새로 저장한다. 존재하지 않는 ID이면 ConcertNotFoundException을 던진다.
     */
    @Transactional
    public void updateConcert(Long id, AdminConcertUpdateRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);
        concert.update(request.title(), request.cast(), request.startDate(), request.endDate(),
                request.venueName(), request.posterUrl(), request.price(), request.ticketOpenAt());

        if (request.bookingLinks() != null) {
            replaceBookingLinks(id, request.bookingLinks());
        }

        if (request.imageUrls() != null) {
            replaceConcertImages(id, request.imageUrls());
        }
    }

    /**
     * 어드민이 공연을 직접 등록한다. KOPIS 연동 없이 저장되며, 초기 상태는 날짜 기준으로 계산된다.
     *
     * @throws ConcertAlreadyExistsException title·startDate·endDate가 모두 일치하는 공연이 이미 존재하는 경우
     */
    @Transactional
    public AdminConcertCreateResponse createConcert(AdminConcertCreateRequest request) {
        boolean isDuplicate = concertRepository.existsByTitleAndStartDateAndEndDate(
                request.title(), request.startDate(), request.endDate());
        if (isDuplicate) {
            throw new ConcertAlreadyExistsException();
        }

        ConcertStatus status = computeStatusFromDates(request.startDate(), request.endDate());
        Concert concert = Concert.builder()
                .title(request.title())
                .cast(request.cast())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .venueName(request.venueName())
                .posterUrl(request.posterUrl())
                .price(request.price())
                .status(status)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .ticketOpenAt(request.ticketOpenAt())
                .build();
        Concert saved = concertRepository.save(concert);

        if (request.bookingLinks() != null) {
            replaceBookingLinks(saved.getId(), request.bookingLinks());
        }
        if (request.imageUrls() != null) {
            replaceConcertImages(saved.getId(), request.imageUrls());
        }
        return new AdminConcertCreateResponse(saved.getId());
    }

    private void replaceBookingLinks(Long concertId, List<BookingLinkRequest> bookingLinks) {
        concertBookingLinkRepository.deleteByConcertId(concertId);
        List<ConcertBookingLink> links = bookingLinks.stream()
                .map(link -> ConcertBookingLink.builder()
                        .concertId(concertId)
                        .name(link.name())
                        .url(link.url())
                        .build())
                .toList();
        concertBookingLinkRepository.saveAll(links);
    }

    private void replaceConcertImages(Long concertId, List<String> imageUrls) {
        concertImageRepository.deleteByConcertId(concertId);
        List<ConcertImage> images = new ArrayList<>();
        for (int position = 0; position < imageUrls.size(); position++) {
            images.add(ConcertImage.builder()
                    .concertId(concertId)
                    .url(imageUrls.get(position))
                    .position(position)
                    .build());
        }
        concertImageRepository.saveAll(images);
    }

    /**
     * 공연 상태를 강제 변경한다. 새 상태가 UPCOMING 또는 ONGOING이면 연결된 아티스트의 is_coming을 갱신한다.
     *
     * @throws ConcertNotFoundException 존재하지 않는 공연 ID
     */
    @Transactional
    public void forceChangeConcertState(Long id, AdminConcertStateUpdateRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);
        concert.forceChangeStatus(request.status());

        List<ConcertStatus> activeStatuses = List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING);
        if (activeStatuses.contains(request.status())) {
            List<Long> artistIds = concertArtistRepository.findByConcertId(id).stream()
                    .map(ConcertArtist::getArtistId)
                    .distinct()
                    .toList();
            artistRepository.findAllById(artistIds).forEach(artist ->
                    artist.updateIsComing(concertRepository.existsActiveByArtistId(artist.getId(), activeStatuses)));
        }
    }

    /**
     * EXCLUDED 상태 공연 목록과 각 공연에 연결된 아티스트를 반환한다.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminExcludedConcertResponse> getExcludedConcerts(Pageable pageable) {
        Page<Concert> page = concertRepository.findByStatus(ConcertStatus.EXCLUDED, pageable);

        List<Long> concertIds = page.getContent().stream().map(Concert::getId).toList();
        Map<Long, List<ConcertArtist>> artistsByConcertId = concertArtistRepository
                .findByConcertIdIn(concertIds).stream()
                .collect(Collectors.groupingBy(ConcertArtist::getConcertId));

        List<Long> artistIds = artistsByConcertId.values().stream()
                .flatMap(List::stream)
                .map(ConcertArtist::getArtistId)
                .distinct()
                .toList();
        Map<Long, String> artistNameById = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Artist::getName));
        Map<Long, String> koreanNameMap = buildKoreanNameMap(artistIds);

        List<AdminExcludedConcertResponse> content = page.getContent().stream()
                .map(concert -> {
                    List<AdminExcludedArtistResponse> artists = artistsByConcertId
                            .getOrDefault(concert.getId(), List.of()).stream()
                            .map(ca -> new AdminExcludedArtistResponse(
                                    ca.getArtistId(),
                                    artistNameById.get(ca.getArtistId()),
                                    koreanNameMap.get(ca.getArtistId())))
                            .toList();
                    return AdminExcludedConcertResponse.of(concert, artists);
                })
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * PENDING 상태 공연 목록과 각 공연의 후보 아티스트를 반환한다.
     */
    @Transactional(readOnly = true)
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
        Map<Long, String> koreanNameMap = buildKoreanNameMap(artistIds);

        Map<Long, List<ConcertBookingLink>> linksByConcertId = concertBookingLinkRepository
                .findByConcertIdIn(concertIds).stream()
                .collect(Collectors.groupingBy(ConcertBookingLink::getConcertId));

        List<AdminPendingConcertResponse> content = page.getContent().stream()
                .map(concert -> {
                    List<AdminCandidateArtistResponse> candidates = candidatesByConcertId
                            .getOrDefault(concert.getId(), List.of()).stream()
                            .map(c -> new AdminCandidateArtistResponse(
                                    c.getArtistId(),
                                    artistNameById.get(c.getArtistId()),
                                    koreanNameMap.get(c.getArtistId())))
                            .toList();
                    List<ConcertBookingLink> links = linksByConcertId.getOrDefault(concert.getId(), List.of());
                    return AdminPendingConcertResponse.of(concert, links, candidates);
                })
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * PENDING 공연을 승인한다. 후보 아티스트를 concert_artist로 이동하고, 날짜 기반으로 상태를 계산하며, 연결된 아티스트의 is_coming을 갱신한다.
     * request가 제공된 경우 ticketOpenAt과 bookingLinks를 함께 저장한다.
     *
     * @param request 티켓 오픈 일시·예매 링크 (선택, null 가능)
     * @throws ConcertNotFoundException   존재하지 않는 공연 ID
     * @throws ConcertNotPendingException 공연이 PENDING 상태가 아닌 경우
     */
    @Transactional
    public void approveConcert(Long concertId, AdminConcertApproveRequest request) {
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

        if (request != null) {
            if (request.ticketOpenAt() != null) {
                concert.update(null, null, null, null, null, null, null, request.ticketOpenAt());
            }
            if (request.bookingLinks() != null && !request.bookingLinks().isEmpty()) {
                concertBookingLinkRepository.deleteByConcertId(concertId);
                List<ConcertBookingLink> links = request.bookingLinks().stream()
                        .map(link -> ConcertBookingLink.builder()
                                .concertId(concertId)
                                .name(link.name())
                                .url(link.url())
                                .build())
                        .toList();
                concertBookingLinkRepository.saveAll(links);
            }
        }
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
     * PENDING 공연에 후보 아티스트를 추가한다. concert_artist_candidate에 저장한다.
     *
     * @throws ConcertNotFoundException            존재하지 않는 공연 ID
     * @throws ConcertNotPendingException          공연이 PENDING 상태가 아닌 경우
     * @throws ArtistNotFoundException             존재하지 않는 아티스트 ID
     * @throws ConcertArtistAlreadyExistsException 이미 후보로 등록된 아티스트
     */
    @Transactional
    public void assignCandidateToConcert(Long concertId, Long artistId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        if (concert.getStatus() != ConcertStatus.PENDING) {
            throw new ConcertNotPendingException();
        }
        if (!artistRepository.existsById(artistId)) {
            throw new ArtistNotFoundException();
        }
        if (concertArtistCandidateRepository.existsByConcertIdAndArtistId(concertId, artistId)) {
            throw new ConcertArtistAlreadyExistsException();
        }
        concertArtistCandidateRepository.save(ConcertArtistCandidate.builder()
                .concertId(concertId)
                .artistId(artistId)
                .build());
    }

    /**
     * PENDING 공연에서 후보 아티스트를 제거한다. concert_artist_candidate에서 삭제한다.
     *
     * @throws ConcertNotFoundException       존재하지 않는 공연 ID
     * @throws ConcertNotPendingException     공연이 PENDING 상태가 아닌 경우
     * @throws ArtistNotFoundException        존재하지 않는 아티스트 ID
     * @throws ConcertArtistNotFoundException 해당 공연의 후보로 등록되지 않은 아티스트
     */
    @Transactional
    public void removeCandidateFromConcert(Long concertId, Long artistId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        if (concert.getStatus() != ConcertStatus.PENDING) {
            throw new ConcertNotPendingException();
        }
        if (!artistRepository.existsById(artistId)) {
            throw new ArtistNotFoundException();
        }
        ConcertArtistCandidate candidate = concertArtistCandidateRepository
                .findByConcertIdAndArtistId(concertId, artistId)
                .orElseThrow(ConcertArtistNotFoundException::new);
        concertArtistCandidateRepository.delete(candidate);
    }

    /**
     * 확정 공연(UPCOMING·ONGOING·ENDED)에 아티스트를 직접 매핑한다. concert_artist에 바로 저장한다.
     * 공연 상태가 UPCOMING 또는 ONGOING이면 해당 아티스트의 is_coming을 true로 갱신한다.
     *
     * @throws ConcertNotFoundException            존재하지 않는 공연 ID
     * @throws ConcertIsPendingException           공연이 PENDING 상태인 경우 — /candidates 엔드포인트를 사용해야 함
     * @throws ArtistNotFoundException             존재하지 않는 아티스트 ID
     * @throws ConcertArtistAlreadyExistsException 이미 매핑된 아티스트
     */
    @Transactional
    public void assignArtistToConcert(Long concertId, Long artistId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        if (concert.getStatus() == ConcertStatus.PENDING) {
            throw new ConcertIsPendingException();
        }
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
     * 확정 공연(UPCOMING·ONGOING·ENDED)에서 아티스트 매핑을 제거한다.
     * 공연이 UPCOMING 또는 ONGOING이면 해당 아티스트의 다른 활성 공연 존재 여부로 is_coming을 재계산한다.
     *
     * @throws ConcertNotFoundException       존재하지 않는 공연 ID
     * @throws ConcertIsPendingException      공연이 PENDING 상태인 경우 — /candidates 엔드포인트를 사용해야 함
     * @throws ArtistNotFoundException        존재하지 않는 아티스트 ID
     * @throws ConcertArtistNotFoundException 해당 공연에 매핑되지 않은 아티스트
     */
    @Transactional
    public void removeArtistFromConcert(Long concertId, Long artistId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(ConcertNotFoundException::new);
        if (concert.getStatus() == ConcertStatus.PENDING) {
            throw new ConcertIsPendingException();
        }
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);
        ConcertArtist concertArtist = concertArtistRepository.findByConcertIdAndArtistId(concertId, artistId)
                .orElseThrow(ConcertArtistNotFoundException::new);
        concertArtistRepository.delete(concertArtist);

        List<ConcertStatus> activeStatuses = List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING);
        if (activeStatuses.contains(concert.getStatus())) {
            artist.updateIsComing(concertRepository.existsActiveByArtistId(artistId, activeStatuses));
        }
    }

    /**
     * DB 등록 아티스트를 이름(별칭 포함)으로 검색해 id·name만 반환한다.
     */
    public PageResponse<AdminArtistSearchResult> searchLocalArtists(String name, Pageable pageable) {
        return PageResponse.from(
                artistRepository.findByNameOrAliasContainingIgnoreCase(name, pageable)
                        .map(artist -> new AdminArtistSearchResult(artist.getId(), artist.getName()))
        );
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
     * MBID 기반으로 아티스트를 동기 수집한다. 수집 결과를 반환한다.
     * 동일 MBID에 대한 요청이 이미 처리 중이면 Data 파이프라인까지 가지 않고 즉시 거부한다.
     *
     * @throws PipelineConflictException 동일 MBID에 대한 수집 요청이 이미 처리 중인 경우
     */
    public PipelineArtistCollectResult collectArtist(AdminArtistCollectRequest request) {
        String mbid = request.mbid();
        String lockToken = artistCollectLockRepository.tryLock(mbid);
        if (lockToken == null) {
            throw new PipelineConflictException();
        }
        try {
            return dataPipelineClient.collectArtist(mbid);
        } finally {
            artistCollectLockRepository.unlock(mbid, lockToken);
        }
    }

    /**
     * KOPIS ID 기반으로 공연을 동기 수집한다. 수집 결과를 반환한다.
     */
    public PipelineConcertCollectResult collectConcert(AdminConcertCollectRequest request) {
        return dataPipelineClient.collectConcert(request.kopisId());
    }

    /**
     * Data 파이프라인에 특정 아티스트의 릴리즈 수집을 트리거한다.
     */
    public void triggerArtistReleases(Long artistId) {
        dataPipelineClient.triggerArtistReleases(artistId);
    }

    /**
     * 특정 공연의 셋리스트를 동기 수집한다. 수집 결과를 반환한다.
     */
    public PipelineSetlistCollectResult triggerConcertSetlist(Long concertId) {
        return dataPipelineClient.collectConcertSetlist(concertId);
    }

    private Map<Long, String> buildKoreanNameMap(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistAliasRepository.findByArtistIdInAndLocale(artistIds, "ko").stream()
                .sorted(java.util.Comparator.comparingLong(ArtistAlias::getId))
                .collect(Collectors.toMap(ArtistAlias::getArtistId, ArtistAlias::getName, (existing, replacement) -> existing));
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
