package com.Coming.Backend.concert.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ConcertBookingLinkRepository concertBookingLinkRepository;
    private final ArtistRepository artistRepository;
    private final UserConcertCalendarRepository userConcertCalendarRepository;

    /**
     * 공연 목록을 status 조건으로 조회한다. 기본 정렬은 startDate desc.
     *
     * @param status null이면 전체 조회
     */
    public PageResponse<ConcertSummaryResponse> getConcerts(ConcertStatus status, Pageable pageable) {
        Page<Concert> page = concertRepository.findConcerts(status, pageable);

        List<Long> concertIds = page.getContent().stream().map(Concert::getId).toList();
        Map<Long, Long> concertToArtistId = buildConcertArtistIdMap(concertIds);
        Map<Long, String> artistNameMap = buildArtistNameMap(new java.util.HashSet<>(concertToArtistId.values()));

        return PageResponse.from(page.map(concert -> {
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
                    concert.getStatus()
            );
        }));
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
                .findFirstByConcertIdAndConfidence(id, "HIGH").orElse(null);
        Long artistId = highConfidenceArtist != null ? highConfidenceArtist.getArtistId() : null;
        String artistName = artistId != null
                ? artistRepository.findById(artistId).map(Artist::getName).orElse(null)
                : null;

        List<TicketLinkDto> ticketLinks = concertBookingLinkRepository.findByConcertId(id).stream()
                .map(link -> new TicketLinkDto(link.getId(), link.getName(), link.getUrl()))
                .toList();

        List<String> posterUrls = concert.getPosterUrl() != null
                ? List.of(concert.getPosterUrl())
                : List.of();

        boolean isInCalendar = userId != null &&
                userConcertCalendarRepository.existsByUserIdAndConcertId(userId, id);

        return new ConcertDetailResponse(
                concert.getId(),
                concert.getPosterUrl(),
                posterUrls,
                artistName,
                artistId,
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                concert.getStatus(),
                concert.getPrice(),
                isInCalendar,
                ticketLinks
        );
    }

    private Map<Long, Long> buildConcertArtistIdMap(List<Long> concertIds) {
        if (concertIds.isEmpty()) {
            return Map.of();
        }
        return concertArtistRepository.findByConcertIdInAndConfidence(concertIds, "HIGH").stream()
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
