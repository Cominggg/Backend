package com.Coming.Backend.user.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.dto.ConcertArtistDto;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.user.dto.ConcertHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.Coming.Backend.concert.entity.ConcertStatus.EXCLUDED;
import static com.Coming.Backend.concert.entity.ConcertStatus.PENDING;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final List<ConcertStatus> HIDDEN_STATUSES = List.of(EXCLUDED, PENDING);

    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;

    /**
     * 내 캘린더에 저장한 공연 중 이미 종료된 공연 목록을 페이지네이션으로 조회한다.
     */
    public PageResponse<ConcertHistoryResponse> getHistory(Long userId, Pageable pageable) {
        Page<Concert> page = concertRepository.findPastByUserCalendar(userId, LocalDate.now(), HIDDEN_STATUSES, pageable);
        List<ConcertHistoryResponse> content = toHistoryList(page.getContent());
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private List<ConcertHistoryResponse> toHistoryList(List<Concert> concerts) {
        if (concerts.isEmpty()) {
            return List.of();
        }
        List<Long> concertIds = concerts.stream().map(Concert::getId).toList();
        Map<Long, List<Long>> concertToArtistIds = buildConcertToArtistIdsMap(concertIds);
        Set<Long> artistIds = concertToArtistIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, String> artistNameMap = buildArtistNameMap(artistIds);
        Map<Long, String> koreanNameMap = buildKoreanNameMap(List.copyOf(artistIds));
        return concerts.stream().map(concert -> {
            List<ConcertArtistDto> artists = concertToArtistIds.getOrDefault(concert.getId(), List.of()).stream()
                    .map(id -> new ConcertArtistDto(id, artistNameMap.get(id), koreanNameMap.get(id)))
                    .toList();
            return new ConcertHistoryResponse(
                    concert.getId(),
                    artists,
                    concert.getTitle(),
                    concert.getStartDate(),
                    concert.getEndDate(),
                    concert.getVenueName(),
                    concert.getStatus().name()
            );
        }).toList();
    }

    private Map<Long, List<Long>> buildConcertToArtistIdsMap(List<Long> concertIds) {
        return concertArtistRepository.findByConcertIdIn(concertIds).stream()
                .collect(Collectors.groupingBy(
                        ConcertArtist::getConcertId,
                        Collectors.mapping(ConcertArtist::getArtistId, Collectors.toList())
                ));
    }

    private Map<Long, String> buildArtistNameMap(Set<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Artist::getName));
    }

    private Map<Long, String> buildKoreanNameMap(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistAliasRepository.findByArtistIdInAndLocale(artistIds, "ko").stream()
                .sorted(Comparator.comparingLong(ArtistAlias::getId))
                .collect(Collectors.toMap(ArtistAlias::getArtistId, ArtistAlias::getName, (existing, replacement) -> existing));
    }
}
