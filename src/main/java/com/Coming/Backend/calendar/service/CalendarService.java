package com.Coming.Backend.calendar.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.concert.dto.ConcertArtistDto;
import com.Coming.Backend.calendar.entity.UserConcertCalendar;
import com.Coming.Backend.calendar.exception.AlreadyInCalendarException;
import com.Coming.Backend.calendar.exception.NotInCalendarException;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.Coming.Backend.concert.entity.ConcertStatus.EXCLUDED;
import static com.Coming.Backend.concert.entity.ConcertStatus.PENDING;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final List<ConcertStatus> HIDDEN_STATUSES = List.of(EXCLUDED, PENDING);

    private final ConcertRepository concertRepository;
    private final ConcertArtistRepository concertArtistRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;
    private final UserConcertCalendarRepository userConcertCalendarRepository;

    /**
     * 특정 연·월에 해당하는 공연 및 티켓팅 일정을 조회한다. 공연은 startDate 기준, 티켓팅은 ticketOpenAt 기준으로 포함된다.
     * type 필드로 구분("CONCERT" | "TICKETING")하며, 날짜 기준 오름차순으로 정렬된다.
     *
     * @param userId 인증된 사용자 ID (null이면 isInCalendar false)
     */
    public List<CalendarEntryResponse> getCalendar(int year, int month, Long userId) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        List<Concert> concertList = concertRepository.findByDateRange(firstDay, lastDay, HIDDEN_STATUSES);
        List<Concert> ticketingList = concertRepository.findByTicketOpenAtRange(
                firstDay.atStartOfDay(), lastDay.plusDays(1).atStartOfDay(), HIDDEN_STATUSES);

        List<Long> allConcertIds = Stream.concat(concertList.stream(), ticketingList.stream())
                .map(Concert::getId).distinct().toList();
        Map<Long, List<Long>> concertToArtistIds = buildConcertToArtistIdsMap(allConcertIds);
        Set<Long> artistIds = concertToArtistIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, String> artistNameMap = buildArtistNameMap(artistIds);
        Map<Long, String> koreanNameMap = buildKoreanNameMap(List.copyOf(artistIds));
        Set<Long> userCalendarIds = resolveUserCalendarIdsById(userId, allConcertIds);

        List<CalendarEntryResponse> result = new ArrayList<>();
        for (Concert concert : concertList) {
            result.add(buildCalendarEntry(concert, "CONCERT", concertToArtistIds, artistNameMap, koreanNameMap, userCalendarIds));
        }
        for (Concert concert : ticketingList) {
            result.add(buildCalendarEntry(concert, "TICKETING", concertToArtistIds, artistNameMap, koreanNameMap, userCalendarIds));
        }
        result.sort(Comparator.comparing(e -> "TICKETING".equals(e.type())
                ? e.ticketOpenAt().toLocalDate()
                : e.startDate()));
        return result;
    }

    /**
     * 내 캘린더에 저장된 종료되지 않은 공연 목록을 페이지네이션으로 조회한다.
     */
    public PageResponse<CalendarEntryResponse> getMyCalendar(Long userId, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<Concert> page = concertRepository.findUpcomingByUserCalendar(userId, today, HIDDEN_STATUSES, pageable);
        List<Long> concertIds = page.getContent().stream().map(Concert::getId).toList();
        Map<Long, List<Long>> concertToArtistIds = buildConcertToArtistIdsMap(concertIds);
        Set<Long> artistIds = concertToArtistIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, String> artistNameMap = buildArtistNameMap(artistIds);
        Map<Long, String> koreanNameMap = buildKoreanNameMap(List.copyOf(artistIds));
        Set<Long> calendarIds = new HashSet<>(concertIds);
        List<CalendarEntryResponse> content = page.getContent().stream()
                .map(c -> buildCalendarEntry(c, "CONCERT", concertToArtistIds, artistNameMap, koreanNameMap, calendarIds))
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * 내 캘린더에 공연을 추가한다.
     *
     * @throws ConcertNotFoundException   존재하지 않는 공연
     * @throws AlreadyInCalendarException 이미 캘린더에 추가된 공연
     */
    @Transactional
    public void addToCalendar(Long userId, Long concertId) {
        if (!concertRepository.existsById(concertId)) {
            throw new ConcertNotFoundException();
        }
        if (userConcertCalendarRepository.existsByUserIdAndConcertId(userId, concertId)) {
            throw new AlreadyInCalendarException();
        }
        try {
            userConcertCalendarRepository.save(
                    UserConcertCalendar.builder()
                            .userId(userId)
                            .concertId(concertId)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyInCalendarException();
        }
    }

    /**
     * 내 캘린더에서 공연을 제거한다.
     *
     * @throws NotInCalendarException 캘린더에 없는 공연
     */
    @Transactional
    public void removeFromCalendar(Long userId, Long concertId) {
        UserConcertCalendar entry = userConcertCalendarRepository
                .findByUserIdAndConcertId(userId, concertId)
                .orElseThrow(NotInCalendarException::new);
        userConcertCalendarRepository.delete(entry);
    }

    private Set<Long> resolveUserCalendarIdsById(Long userId, List<Long> concertIds) {
        if (userId == null || concertIds.isEmpty()) {
            return Set.of();
        }
        return userConcertCalendarRepository.findByUserIdAndConcertIdIn(userId, concertIds).stream()
                .map(UserConcertCalendar::getConcertId)
                .collect(Collectors.toSet());
    }

    private CalendarEntryResponse buildCalendarEntry(Concert concert, String type,
                                                      Map<Long, List<Long>> concertToArtistIds,
                                                      Map<Long, String> artistNameMap,
                                                      Map<Long, String> koreanNameMap,
                                                      Set<Long> userCalendarIds) {
        List<ConcertArtistDto> artists = concertToArtistIds.getOrDefault(concert.getId(), List.of()).stream()
                .map(id -> new ConcertArtistDto(id, artistNameMap.get(id), koreanNameMap.get(id)))
                .toList();
        return new CalendarEntryResponse(
                concert.getId(),
                type,
                artists,
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getStatus().name(),
                concert.getPosterUrl(),
                concert.getVenueName(),
                userCalendarIds.contains(concert.getId()),
                concert.getTicketOpenAt()
        );
    }

    private Map<Long, List<Long>> buildConcertToArtistIdsMap(List<Long> concertIds) {
        if (concertIds.isEmpty()) {
            return Map.of();
        }
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
