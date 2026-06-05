package com.Coming.Backend.my.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.my.dto.ConcertHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyService {

    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ArtistRepository artistRepository;

    /**
     * 내 캘린더에 저장한 공연 중 이미 종료된 공연 목록을 페이지네이션으로 조회한다.
     */
    public PageResponse<ConcertHistoryResponse> getHistory(Long userId, Pageable pageable) {
        Page<Concert> page = concertRepository.findPastByUserCalendar(userId, LocalDate.now(), ConcertStatus.EXCLUDED, pageable);
        List<ConcertHistoryResponse> content = toHistoryList(page.getContent());
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private List<ConcertHistoryResponse> toHistoryList(List<Concert> concerts) {
        if (concerts.isEmpty()) {
            return List.of();
        }
        List<Long> concertIds = concerts.stream().map(Concert::getId).toList();
        Map<Long, Long> concertToArtistId = buildConcertArtistIdMap(concertIds);
        Map<Long, String> artistNameMap = buildArtistNameMap(new HashSet<>(concertToArtistId.values()));
        return concerts.stream().map(concert -> {
            Long artistId = concertToArtistId.get(concert.getId());
            String artistName = artistId != null ? artistNameMap.get(artistId) : null;
            return new ConcertHistoryResponse(
                    concert.getId(),
                    artistName,
                    concert.getTitle(),
                    concert.getStartDate(),
                    concert.getEndDate(),
                    concert.getVenueName(),
                    concert.getStatus().name()
            );
        }).toList();
    }

    private Map<Long, Long> buildConcertArtistIdMap(List<Long> concertIds) {
        return concertArtistRepository.findByConcertIdIn(concertIds).stream()
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
