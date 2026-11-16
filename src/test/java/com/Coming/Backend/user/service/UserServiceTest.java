package com.Coming.Backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.calendar.dto.CalendarEntryResponse;
import com.Coming.Backend.calendar.service.CalendarService;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import com.Coming.Backend.user.dto.ConcertHistoryResponse;
import com.Coming.Backend.user.dto.MySummaryResponse;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ConcertArtistRepository concertArtistRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistAliasRepository artistAliasRepository;

    @Mock
    private UserFollowArtistRepository userFollowArtistRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private CalendarService calendarService;

    private static final Long CONCERT_ID = 1L;
    private static final Long ARTIST_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private Concert buildPastConcert(Long id) {
        return Concert.builder()
                .id(id)
                .kopisId("kopis-" + id)
                .title("공연 " + id)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(8))
                .venueName("KSPO DOME, 서울")
                .posterUrl("https://example.com/poster.jpg")
                .price("전석 99,000원")
                .status(ConcertStatus.ENDED)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .build();
    }

    private Artist buildArtist(Long id, String name) {
        return Artist.builder()
                .id(id)
                .mbid("mbid-" + id)
                .name(name)
                .isComing(false)
                .build();
    }

    private ConcertArtist buildConcertArtist(Long concertId, Long artistId) {
        return ConcertArtist.builder()
                .concertId(concertId)
                .artistId(artistId)
                .build();
    }

    // -------------------------------------------------------------------------
    // getHistory
    // -------------------------------------------------------------------------

    @Test
    void should_return_past_concerts_with_artist_name_when_user_has_history() {
        // given
        Concert concert = buildPastConcert(CONCERT_ID);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findPastByUserCalendar(eq(USER_ID), any(LocalDate.class), anyList(), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        PageResponse<ConcertHistoryResponse> result = userService.getHistory(USER_ID, PAGEABLE);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(CONCERT_ID);
        assertThat(result.content().get(0).artists()).hasSize(1);
        assertThat(result.content().get(0).artists().get(0).name()).isEqualTo("YOASOBI");
        assertThat(result.content().get(0).title()).isEqualTo("공연 1");
        assertThat(result.content().get(0).venue()).isEqualTo("KSPO DOME, 서울");
        assertThat(result.content().get(0).posterUrl()).isEqualTo("https://example.com/poster.jpg");
        assertThat(result.content().get(0).status()).isEqualTo("ENDED");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    void should_return_empty_page_when_user_has_no_past_concerts() {
        // given
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);

        given(concertRepository.findPastByUserCalendar(eq(USER_ID), any(LocalDate.class), anyList(), eq(PAGEABLE)))
                .willReturn(emptyPage);

        // when
        PageResponse<ConcertHistoryResponse> result = userService.getHistory(USER_ID, PAGEABLE);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void should_return_null_artist_name_when_concert_has_no_matched_artist() {
        // given
        Concert concert = buildPastConcert(CONCERT_ID);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findPastByUserCalendar(eq(USER_ID), any(LocalDate.class), anyList(), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        PageResponse<ConcertHistoryResponse> result = userService.getHistory(USER_ID, PAGEABLE);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).artists()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getSummary
    // -------------------------------------------------------------------------

    private static final Pageable FIRST_UPCOMING = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "startDate"));
    private static final Pageable FIRST_ONLY = PageRequest.of(0, 1);

    private CalendarEntryResponse buildCalendarEntry(Long concertId) {
        return new CalendarEntryResponse(
                concertId, "CONCERT", List.of(), "공연 " + concertId,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(6),
                "UPCOMING", "https://example.com/poster.jpg", "KSPO DOME, 서울", true, null);
    }

    private void givenSummarySources(PageResponse<CalendarEntryResponse> upcoming, long historyTotal) {
        given(calendarService.getMyCalendar(eq(USER_ID), any(Pageable.class))).willReturn(upcoming);
        given(concertRepository.findPastByUserCalendar(eq(USER_ID), any(LocalDate.class), anyList(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(buildPastConcert(CONCERT_ID)), FIRST_ONLY, historyTotal));
        given(userFollowArtistRepository.countByUserId(USER_ID)).willReturn(7L);
        given(inquiryRepository.countByUserIdAndStatus(eq(USER_ID), any(InquiryStatus.class))).willReturn(2L);
    }

    @Test
    void should_return_counts_and_next_concert_when_user_has_data() {
        // given
        CalendarEntryResponse next = buildCalendarEntry(CONCERT_ID);
        givenSummarySources(new PageResponse<>(List.of(next), 0, 1, 3L, 3), 5L);

        // when
        MySummaryResponse result = userService.getSummary(USER_ID);

        // then
        assertThat(result.followingCount()).isEqualTo(7L);
        assertThat(result.upcomingCount()).isEqualTo(3L);
        assertThat(result.nextConcert()).isEqualTo(next);
        assertThat(result.historyCount()).isEqualTo(5L);
        assertThat(result.inProgressInquiryCount()).isEqualTo(2L);
    }

    @Test
    void should_return_null_next_concert_when_no_upcoming_concert() {
        // given
        givenSummarySources(new PageResponse<>(List.of(), 0, 1, 0L, 0), 5L);

        // when
        MySummaryResponse result = userService.getSummary(USER_ID);

        // then
        assertThat(result.nextConcert()).isNull();
        assertThat(result.upcomingCount()).isZero();
    }

    @Test
    void should_query_with_first_page_and_in_progress_status_when_get_summary() {
        // given
        givenSummarySources(new PageResponse<>(List.of(), 0, 1, 0L, 0), 0L);

        // when
        userService.getSummary(USER_ID);

        // then
        verify(calendarService).getMyCalendar(USER_ID, FIRST_UPCOMING);
        verify(concertRepository).findPastByUserCalendar(eq(USER_ID), any(LocalDate.class), anyList(), eq(FIRST_ONLY));
        verify(inquiryRepository).countByUserIdAndStatus(USER_ID, InquiryStatus.IN_PROGRESS);
    }
}
