package com.Coming.Backend.calendar.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
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
    private final UserConcertCalendarRepository userConcertCalendarRepository;

    /**
     * 특정 연·월에 해당하는 전체 공연 목록을 조회한다. 인증된 사용자는 각 공연의 캘린더 추가 여부를 포함한다.
     *
     * @param userId 인증된 사용자 ID (null이면 isInCalendar false)
     */
    public List<CalendarEntryResponse> getCalendar(int year, int month, Long userId) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        List<Concert> concerts = concertRepository.findByDateRange(firstDay, lastDay, HIDDEN_STATUSES);
        Set<Long> userCalendarIds = resolveUserCalendarIds(userId, concerts);
        return toCalendarEntryList(concerts, userCalendarIds);
    }

    /**
     * 내 캘린더에 저장된 공연 목록을 페이지네이션으로 조회한다.
     */
    public PageResponse<CalendarEntryResponse> getMyCalendar(Long userId, Pageable pageable) {
        Page<Concert> page = concertRepository.findByUserCalendar(userId, HIDDEN_STATUSES, pageable);
        Set<Long> allIds = page.getContent().stream().map(Concert::getId).collect(Collectors.toSet());
        List<CalendarEntryResponse> content = toCalendarEntryList(page.getContent(), allIds);
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
        userConcertCalendarRepository.save(
                UserConcertCalendar.builder()
                        .userId(userId)
                        .concertId(concertId)
                        .build()
        );
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

    private Set<Long> resolveUserCalendarIds(Long userId, List<Concert> concerts) {
        if (userId == null || concerts.isEmpty()) {
            return Set.of();
        }
        List<Long> concertIds = concerts.stream().map(Concert::getId).toList();
        return userConcertCalendarRepository.findByUserIdAndConcertIdIn(userId, concertIds).stream()
                .map(UserConcertCalendar::getConcertId)
                .collect(Collectors.toSet());
    }

    private List<CalendarEntryResponse> toCalendarEntryList(List<Concert> concerts, Set<Long> userCalendarIds) {
        if (concerts.isEmpty()) {
            return List.of();
        }
        List<Long> concertIds = concerts.stream().map(Concert::getId).toList();
        Map<Long, Long> concertToArtistId = buildConcertArtistIdMap(concertIds);
        Map<Long, String> artistNameMap = buildArtistNameMap(new HashSet<>(concertToArtistId.values()));
        return concerts.stream().map(concert -> {
            Long artistId = concertToArtistId.get(concert.getId());
            String artistName = artistId != null ? artistNameMap.get(artistId) : null;
            return new CalendarEntryResponse(
                    concert.getId(),
                    artistName,
                    concert.getTitle(),
                    concert.getStartDate(),
                    concert.getEndDate(),
                    concert.getStatus().name(),
                    concert.getPosterUrl(),
                    concert.getVenueName(),
                    userCalendarIds.contains(concert.getId())
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
