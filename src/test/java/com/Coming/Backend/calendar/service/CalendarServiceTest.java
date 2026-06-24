package com.Coming.Backend.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.calendar.entity.UserConcertCalendar;
import com.Coming.Backend.calendar.exception.AlreadyInCalendarException;
import com.Coming.Backend.calendar.exception.NotInCalendarException;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @InjectMocks
    private CalendarService calendarService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ConcertArtistRepository concertArtistRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private UserConcertCalendarRepository userConcertCalendarRepository;

    private static final Long CONCERT_ID = 1L;
    private static final Long ARTIST_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private Concert buildConcert(Long id) {
        return Concert.builder()
                .id(id)
                .kopisId("kopis-" + id)
                .title("공연 " + id)
                .startDate(LocalDate.of(2025, 8, 15))
                .endDate(LocalDate.of(2025, 8, 16))
                .venueName("KSPO DOME, 서울")
                .posterUrl("https://example.com/poster.jpg")
                .price("전석 99,000원")
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .build();
    }

    private Artist buildArtist(Long id, String name) {
        return Artist.builder()
                .id(id)
                .mbid("mbid-" + id)
                .name(name)
                .isComing(true)
                .build();
    }

    private ConcertArtist buildConcertArtist(Long concertId, Long artistId) {
        return ConcertArtist.builder()
                .concertId(concertId)
                .artistId(artistId)
                .build();
    }

    // -------------------------------------------------------------------------
    // getCalendar
    // -------------------------------------------------------------------------

    @Test
    void should_return_concert_list_with_artist_name_when_concerts_exist_in_month() {
        // given
        Concert concert = buildConcert(CONCERT_ID);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");

        given(concertRepository.findByDateRange(
                eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2025, 8, 31)),
                anyList()))
                .willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, USER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).concertId()).isEqualTo(CONCERT_ID);
        assertThat(result.get(0).type()).isEqualTo("CONCERT");
        assertThat(result.get(0).artistName()).isEqualTo("YOASOBI");
        assertThat(result.get(0).status()).isEqualTo("UPCOMING");
        assertThat(result.get(0).venue()).isEqualTo("KSPO DOME, 서울");
        assertThat(result.get(0).isInCalendar()).isFalse();
    }

    @Test
    void should_return_is_in_calendar_true_when_user_has_concert_in_calendar() {
        // given
        Concert concert = buildConcert(CONCERT_ID);
        UserConcertCalendar entry = UserConcertCalendar.builder()
                .userId(USER_ID).concertId(CONCERT_ID).build();

        given(concertRepository.findByDateRange(any(), any(), anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of(entry));

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, USER_ID);

        // then
        assertThat(result.get(0).isInCalendar()).isTrue();
    }

    @Test
    void should_return_is_in_calendar_false_when_user_is_not_authenticated() {
        // given
        Concert concert = buildConcert(CONCERT_ID);

        given(concertRepository.findByDateRange(any(), any(), anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, null);

        // then
        assertThat(result.get(0).isInCalendar()).isFalse();
    }

    @Test
    void should_return_empty_list_when_no_concerts_in_month() {
        // given
        given(concertRepository.findByDateRange(any(), any(), anyList())).willReturn(List.of());

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_ticketing_type_when_concert_has_ticket_open_at_in_month() {
        // given
        LocalDateTime ticketOpenAt = LocalDateTime.of(2025, 8, 10, 10, 0);
        Concert concert = Concert.builder()
                .id(CONCERT_ID)
                .kopisId("kopis-1")
                .title("공연 1")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 1))
                .venueName("KSPO DOME, 서울")
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .ticketOpenAt(ticketOpenAt)
                .build();

        given(concertRepository.findByDateRange(any(), any(), anyList())).willReturn(List.of());
        given(concertRepository.findByTicketOpenAtMonth(eq(2025), eq(8), anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("TICKETING");
        assertThat(result.get(0).ticketOpenAt()).isEqualTo(ticketOpenAt);
    }

    @Test
    void should_sort_ticketing_before_concert_when_ticket_open_at_is_earlier_in_month() {
        // given
        Concert concertEntry = buildConcert(1L);  // startDate = 2025-08-15
        Concert ticketingEntry = Concert.builder()
                .id(2L)
                .kopisId("kopis-2")
                .title("공연 2")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 1))
                .venueName("KSPO DOME, 서울")
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .ticketOpenAt(LocalDateTime.of(2025, 8, 5, 10, 0))  // 8월 5일 (15일보다 앞)
                .build();

        given(concertRepository.findByDateRange(any(), any(), anyList())).willReturn(List.of(concertEntry));
        given(concertRepository.findByTicketOpenAtMonth(eq(2025), eq(8), anyList())).willReturn(List.of(ticketingEntry));
        given(concertArtistRepository.findByConcertIdIn(any())).willReturn(List.of());

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, null);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo("TICKETING");  // 8월 5일
        assertThat(result.get(1).type()).isEqualTo("CONCERT");     // 8월 15일
    }

    @Test
    void should_return_both_types_when_same_concert_has_start_date_and_ticket_open_at_in_same_month() {
        // given
        LocalDateTime ticketOpenAt = LocalDateTime.of(2025, 8, 5, 10, 0);
        Concert concert = Concert.builder()
                .id(CONCERT_ID)
                .kopisId("kopis-1")
                .title("공연 1")
                .startDate(LocalDate.of(2025, 8, 15))
                .endDate(LocalDate.of(2025, 8, 15))
                .venueName("KSPO DOME, 서울")
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .ticketOpenAt(ticketOpenAt)
                .build();

        given(concertRepository.findByDateRange(any(), any(), anyList())).willReturn(List.of(concert));
        given(concertRepository.findByTicketOpenAtMonth(eq(2025), eq(8), anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<CalendarEntryResponse> result = calendarService.getCalendar(2025, 8, null);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(e -> "TICKETING".equals(e.type())).count()).isEqualTo(1);
        assertThat(result.stream().filter(e -> "CONCERT".equals(e.type())).count()).isEqualTo(1);
        assertThat(result.stream().allMatch(e -> e.concertId().equals(CONCERT_ID))).isTrue();
        assertThat(result.get(0).type()).isEqualTo("TICKETING");  // 8월 5일 (15일보다 앞)
        assertThat(result.get(1).type()).isEqualTo("CONCERT");    // 8월 15일
    }

    // -------------------------------------------------------------------------
    // getMyCalendar
    // -------------------------------------------------------------------------

    @Test
    void should_return_all_calendar_entries_when_upcoming_is_false() {
        // given
        Concert concert = buildConcert(CONCERT_ID);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByUserCalendar(eq(USER_ID), anyList(), eq(PAGEABLE))).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        PageResponse<CalendarEntryResponse> result = calendarService.getMyCalendar(USER_ID, false, PAGEABLE);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).concertId()).isEqualTo(CONCERT_ID);
        assertThat(result.content().get(0).type()).isEqualTo("CONCERT");
        assertThat(result.content().get(0).artistName()).isEqualTo("YOASOBI");
        assertThat(result.content().get(0).isInCalendar()).isTrue();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    void should_return_only_upcoming_concerts_when_upcoming_is_true() {
        // given
        Concert concert = buildConcert(CONCERT_ID);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findUpcomingByUserCalendar(eq(USER_ID), any(LocalDate.class), anyList(), eq(PAGEABLE))).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        PageResponse<CalendarEntryResponse> result = calendarService.getMyCalendar(USER_ID, true, PAGEABLE);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).concertId()).isEqualTo(CONCERT_ID);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // addToCalendar
    // -------------------------------------------------------------------------

    @Test
    void should_save_calendar_entry_when_concert_exists_and_not_already_added() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(userConcertCalendarRepository.existsByUserIdAndConcertId(USER_ID, CONCERT_ID)).willReturn(false);

        // when
        calendarService.addToCalendar(USER_ID, CONCERT_ID);

        // then
        verify(userConcertCalendarRepository).save(any(UserConcertCalendar.class));
    }

    @Test
    void should_throw_concert_not_found_when_concert_does_not_exist_on_add() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> calendarService.addToCalendar(USER_ID, CONCERT_ID))
                .isInstanceOf(ConcertNotFoundException.class)
                .hasMessage(ErrorCode.CONCERT_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_already_in_calendar_when_concert_already_added() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(userConcertCalendarRepository.existsByUserIdAndConcertId(USER_ID, CONCERT_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> calendarService.addToCalendar(USER_ID, CONCERT_ID))
                .isInstanceOf(AlreadyInCalendarException.class)
                .hasMessage(ErrorCode.ALREADY_IN_CALENDAR.getMessage());
    }

    // -------------------------------------------------------------------------
    // removeFromCalendar
    // -------------------------------------------------------------------------

    @Test
    void should_delete_calendar_entry_when_concert_is_in_calendar() {
        // given
        UserConcertCalendar entry = UserConcertCalendar.builder()
                .userId(USER_ID)
                .concertId(CONCERT_ID)
                .build();
        given(userConcertCalendarRepository.findByUserIdAndConcertId(USER_ID, CONCERT_ID))
                .willReturn(Optional.of(entry));

        // when
        calendarService.removeFromCalendar(USER_ID, CONCERT_ID);

        // then
        verify(userConcertCalendarRepository).delete(entry);
    }

    @Test
    void should_throw_not_in_calendar_when_concert_not_in_user_calendar() {
        // given
        given(userConcertCalendarRepository.findByUserIdAndConcertId(USER_ID, CONCERT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> calendarService.removeFromCalendar(USER_ID, CONCERT_ID))
                .isInstanceOf(NotInCalendarException.class)
                .hasMessage(ErrorCode.NOT_IN_CALENDAR.getMessage());
    }
}
