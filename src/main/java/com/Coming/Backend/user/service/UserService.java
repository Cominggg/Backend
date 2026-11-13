package com.Coming.Backend.user.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.calendar.service.CalendarService;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.dto.ConcertArtistDto;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import com.Coming.Backend.user.dto.ConcertHistoryResponse;
import com.Coming.Backend.user.dto.MySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    // GET /api/me/concerts/upcoming 기본 정렬과 같게 둬야 nextConcert가 목록 첫 항목과 일치한다
    private static final Pageable FIRST_UPCOMING = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "startDate"));
    private static final Pageable FIRST_ONLY = PageRequest.of(0, 1);

    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;
    private final InquiryRepository inquiryRepository;
    private final CalendarService calendarService;

    /**
     * 내 캘린더에 저장한 공연 중 이미 종료된 공연 목록을 페이지네이션으로 조회한다.
     */
    public PageResponse<ConcertHistoryResponse> getHistory(Long userId, Pageable pageable) {
        Page<Concert> page = concertRepository.findPastByUserCalendar(userId, LocalDate.now(), HIDDEN_STATUSES, pageable);
        List<ConcertHistoryResponse> content = toHistoryList(page.getContent());
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * MY 첫 화면에 필요한 숫자와 다음 공연 1건을 한 번에 조회한다.
     * 각 숫자는 대응하는 목록 API(팔로잉 아티스트·예정 공연·다녀온 공연·처리 중 문의)의 totalElements와 같은 조건으로 센다.
     */
    public MySummaryResponse getSummary(Long userId) {
        PageResponse<CalendarEntryResponse> upcoming = calendarService.getMyCalendar(userId, FIRST_UPCOMING);
        Page<Concert> history = concertRepository.findPastByUserCalendar(userId, LocalDate.now(), HIDDEN_STATUSES, FIRST_ONLY);
        return new MySummaryResponse(
                userFollowArtistRepository.countByUserId(userId),
                upcoming.totalElements(),
                upcoming.content().stream().findFirst().orElse(null),
                history.getTotalElements(),
                inquiryRepository.countByUserIdAndStatus(userId, InquiryStatus.IN_PROGRESS)
        );
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
                    concert.getPosterUrl(),
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
