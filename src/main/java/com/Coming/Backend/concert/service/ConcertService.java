package com.Coming.Backend.concert.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.calendar.entity.UserConcertCalendar;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.dto.ConcertDetailResponse;
import com.Coming.Backend.concert.dto.ConcertSummaryResponse;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.dto.TicketLinkDto;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertBookingLinkRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private static final String HIGH_CONFIDENCE = "HIGH";

    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ConcertBookingLinkRepository concertBookingLinkRepository;
    private final ArtistRepository artistRepository;
    private final UserConcertCalendarRepository userConcertCalendarRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;

    /**
     * 공연 목록을 status 조건으로 조회한다. 기본 정렬은 startDate desc.
     *
     * @param status null이면 전체 조회
     * @param userId 인증 사용자 ID (null이면 isInCalendar 전부 false)
     */
    public PageResponse<ConcertSummaryResponse> getConcerts(ConcertStatus status, Pageable pageable, Long userId) {
        Page<Concert> page = (status == null)
                ? concertRepository.findAll(pageable)
                : concertRepository.findByStatus(status, pageable);
        List<ConcertSummaryResponse> content = toConcertSummaryList(page.getContent(), userId);
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * 팔로우한 아티스트의 공연 목록을 조회한다. 기본 정렬은 startDate DESC.
     *
     * @param userId 인증된 사용자 ID
     * @param status null이면 전체 조회
     */
    public List<ConcertSummaryResponse> getFollowingConcerts(Long userId, ConcertStatus status) {
        List<Long> artistIds = userFollowArtistRepository.findByUserId(userId).stream()
                .map(UserFollowArtist::getArtistId)
                .toList();
        if (artistIds.isEmpty()) {
            return List.of();
        }
        List<Concert> concerts = (status == null)
                ? concertRepository.findAllByArtistIdIn(artistIds)
                : concertRepository.findAllByArtistIdInAndStatus(artistIds, status);
        return toConcertSummaryList(concerts, userId);
    }

    /**
     * 공연 상세 정보를 조회한다. 조회 시 viewCount가 1 증가한다.
     *
     * @param userId 인증된 사용자 ID (null이면 isInCalendar false)
     */
    @Transactional
    public ConcertDetailResponse getConcert(Long id, Long userId) {
        Concert concert = concertRepository.findById(id).orElseThrow(ConcertNotFoundException::new);
        concertRepository.incrementViewCount(id);

        ConcertArtist highConfidenceArtist = concertArtistRepository
                .findFirstByConcertIdAndConfidence(id, HIGH_CONFIDENCE).orElse(null);
        Long artistId = highConfidenceArtist != null ? highConfidenceArtist.getArtistId() : null;
        boolean isInCalendar = userId != null &&
                userConcertCalendarRepository.existsByUserIdAndConcertId(userId, id);

        return new ConcertDetailResponse(
                concert.getId(),
                concert.getPosterUrl(),
                toPosterUrls(concert.getPosterUrl()),
                resolveArtistName(artistId),
                artistId,
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                concert.getStatus(),
                concert.getPrice(),
                isInCalendar,
                buildTicketLinks(id)
        );
    }

    private List<ConcertSummaryResponse> toConcertSummaryList(List<Concert> concerts, Long userId) {
        if (concerts.isEmpty()) {
            return List.of();
        }
        List<Long> concertIds = concerts.stream().map(Concert::getId).toList();
        Map<Long, Long> concertToArtistId = buildConcertArtistIdMap(concertIds);
        Map<Long, String> artistNameMap = buildArtistNameMap(new HashSet<>(concertToArtistId.values()));
        Set<Long> calendarConcertIds = buildCalendarConcertIds(userId, concertIds);
        return concerts.stream().map(concert -> {
            Long artistId = concertToArtistId.get(concert.getId());
            String artistName = artistId != null ? artistNameMap.get(artistId) : null;
            return new ConcertSummaryResponse(
                    concert.getId(),
                    concert.getPosterUrl(),
                    artistName,
                    concert.getTitle(),
                    concert.getStartDate(),
                    concert.getEndDate(),
                    concert.getVenueName(),
                    concert.getStatus(),
                    calendarConcertIds.contains(concert.getId())
            );
        }).toList();
    }

    private Set<Long> buildCalendarConcertIds(Long userId, List<Long> concertIds) {
        if (userId == null) {
            return Set.of();
        }
        return userConcertCalendarRepository.findByUserIdAndConcertIdIn(userId, concertIds).stream()
                .map(UserConcertCalendar::getConcertId)
                .collect(Collectors.toSet());
    }

    private String resolveArtistName(Long artistId) {
        if (artistId == null) {
            return null;
        }
        return artistRepository.findById(artistId).map(Artist::getName).orElse(null);
    }

    private List<TicketLinkDto> buildTicketLinks(Long concertId) {
        return concertBookingLinkRepository.findByConcertId(concertId).stream()
                .map(link -> new TicketLinkDto(link.getId(), link.getName(), link.getUrl()))
                .toList();
    }

    private List<String> toPosterUrls(String posterUrl) {
        return posterUrl != null ? List.of(posterUrl) : List.of();
    }

    private Map<Long, Long> buildConcertArtistIdMap(List<Long> concertIds) {
        if (concertIds.isEmpty()) {
            return Map.of();
        }
        return concertArtistRepository.findByConcertIdInAndConfidence(concertIds, HIGH_CONFIDENCE).stream()
                .collect(Collectors.toMap(ConcertArtist::getConcertId, ConcertArtist::getArtistId));
    }

    private Map<Long, String> buildArtistNameMap(Set<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Artist::getName));
    }
}
