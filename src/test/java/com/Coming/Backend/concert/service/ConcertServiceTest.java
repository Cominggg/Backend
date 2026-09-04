package com.Coming.Backend.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.calendar.entity.UserConcertCalendar;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.dto.ArtistSummary;
import com.Coming.Backend.concert.dto.ConcertDetailResponse;
import com.Coming.Backend.concert.dto.ConcertStatsResponse;
import com.Coming.Backend.concert.dto.ConcertSummaryResponse;
import com.Coming.Backend.concert.dto.SetlistResponse;
import com.Coming.Backend.concert.entity.Setlist;
import com.Coming.Backend.concert.entity.SetlistTrack;
import com.Coming.Backend.concert.repository.SetlistRepository;
import com.Coming.Backend.concert.repository.SetlistTrackRepository;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertBookingLink;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertBookingLinkRepository;
import com.Coming.Backend.concert.repository.ConcertImageRepository;
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
class ConcertServiceTest {

    @InjectMocks
    private ConcertService concertService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ConcertArtistRepository concertArtistRepository;

    @Mock
    private ConcertBookingLinkRepository concertBookingLinkRepository;

    @Mock
    private ConcertImageRepository concertImageRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistAliasRepository artistAliasRepository;

    @Mock
    private UserConcertCalendarRepository userConcertCalendarRepository;

    @Mock
    private UserFollowArtistRepository userFollowArtistRepository;

    @Mock
    private SetlistRepository setlistRepository;

    @Mock
    private SetlistTrackRepository setlistTrackRepository;

    private static final Long CONCERT_ID = 1L;
    private static final Long ARTIST_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private Concert buildConcert(Long id, ConcertStatus status) {
        return Concert.builder()
                .id(id)
                .kopisId("kopis-" + id)
                .title("공연 " + id)
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 6, 3))
                .venueName("올림픽공원")
                .posterUrl("https://example.com/poster.jpg")
                .price("전석 99,000원")
                .status(status)
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
    // getPopularConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_popular_concerts_with_artist_name_ordered_by_view_count() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");

        given(concertRepository.findTop10Popular(anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).artists()).containsExactly(new ArtistSummary(ARTIST_ID, "YOASOBI", null));
        verify(concertRepository).findTop10Popular(anyList());
    }

    @Test
    void should_return_upcoming_and_ongoing_before_ended_in_popular_list() {
        // given
        Concert upcoming = buildConcert(1L, ConcertStatus.UPCOMING);
        Concert ongoing = buildConcert(2L, ConcertStatus.ONGOING);
        Concert ended = buildConcert(3L, ConcertStatus.ENDED);

        given(concertRepository.findTop10Popular(anyList()))
                .willReturn(List.of(upcoming, ongoing, ended));
        given(concertArtistRepository.findByConcertIdIn(any())).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).status()).isEqualTo(ConcertStatus.UPCOMING);
        assertThat(result.get(1).status()).isEqualTo(ConcertStatus.ONGOING);
        assertThat(result.get(2).status()).isEqualTo(ConcertStatus.ENDED);
    }

    @Test
    void should_return_empty_list_when_no_popular_concerts_exist() {
        // given
        given(concertRepository.findTop10Popular(anyList())).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_is_in_calendar_false_for_popular_concerts_when_user_is_not_authenticated() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);

        given(concertRepository.findTop10Popular(anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result.get(0).isInCalendar()).isFalse();
    }

    @Test
    void should_return_is_in_calendar_true_for_popular_concerts_when_authenticated_user_has_concert_in_calendar() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        UserConcertCalendar calendarEntry = UserConcertCalendar.builder()
                .userId(USER_ID)
                .concertId(CONCERT_ID)
                .build();

        given(concertRepository.findTop10Popular(anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of(calendarEntry));

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(USER_ID);

        // then
        assertThat(result.get(0).isInCalendar()).isTrue();
    }

    @Test
    void should_not_include_excluded_concerts_in_popular_list() {
        // given
        Concert upcomingConcert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findTop10Popular(anyList()))
                .willReturn(List.of(upcomingConcert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        verify(concertRepository).findTop10Popular(anyList());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ConcertStatus.UPCOMING);
    }

    // -------------------------------------------------------------------------
    // getConcertStats
    // -------------------------------------------------------------------------

    @Test
    void should_return_concert_count_when_concerts_exist_in_given_month() {
        // given
        given(concertRepository.countByYearAndMonth(2025, 8)).willReturn(12);

        // when
        ConcertStatsResponse result = concertService.getConcertStats(2025, 8);

        // then
        assertThat(result.concertCount()).isEqualTo(12);
        verify(concertRepository).countByYearAndMonth(2025, 8);
    }

    @Test
    void should_return_zero_when_no_concerts_exist_in_given_month() {
        // given
        given(concertRepository.countByYearAndMonth(2025, 1)).willReturn(0);

        // when
        ConcertStatsResponse result = concertService.getConcertStats(2025, 1);

        // then
        assertThat(result.concertCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // getConcerts — 기본 조회 (q 없음 / q 있음 / status)
    // -------------------------------------------------------------------------

    @Test
    void should_call_find_concerts_when_q_is_not_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");

        given(concertRepository.findConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, null, null, PAGEABLE, null);

        // then
        verify(concertRepository).findConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE));
        verify(concertRepository, never()).searchConcerts(any(), any(), any(), any(), anyBoolean(), any(), any(), any());
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).artists()).containsExactly(new ArtistSummary(ARTIST_ID, "YOASOBI", null));
    }

    @Test
    void should_call_search_concerts_with_lowercased_wrapped_query_when_q_is_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.searchConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq("%iu%"), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts("IU", null, null, null, null, PAGEABLE, null);

        // then
        verify(concertRepository).searchConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq("%iu%"), eq(PAGEABLE));
        verify(concertRepository, never()).findConcerts(any(), any(), any(), any(), anyBoolean(), any(), any());
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_pass_status_filter_to_find_concerts_when_status_is_given() {
        // given
        Page<Concert> page = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findConcerts(anyList(), eq(ConcertStatus.UPCOMING), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, ConcertStatus.UPCOMING, null, null, null, PAGEABLE, null);

        // then
        verify(concertRepository).findConcerts(anyList(), eq(ConcertStatus.UPCOMING), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE));
        assertThat(response.content()).isEmpty();
    }

    @Test
    void should_return_empty_page_when_no_concerts_match_filter() {
        // given
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(emptyPage);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, null, null, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void should_return_empty_page_when_status_is_excluded() {
        // given
        // EXCLUDED는 리포지토리 쿼리 내 status NOT IN :hidden 조건에 의해 항상 걸러진다
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findConcerts(anyList(), eq(ConcertStatus.EXCLUDED), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(emptyPage);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, ConcertStatus.EXCLUDED, null, null, null, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(concertRepository).findConcerts(anyList(), eq(ConcertStatus.EXCLUDED), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE));
    }

    // -------------------------------------------------------------------------
    // getConcerts — inCalendar
    // -------------------------------------------------------------------------

    @Test
    void should_return_empty_page_when_in_calendar_true_and_user_not_authenticated() {
        // given
        // userId == null이면 레포를 호출하지 않고 즉시 빈 페이지를 반환한다

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, true, null, null, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(concertRepository, never()).findConcerts(any(), any(), any(), any(), anyBoolean(), any(), any());
        verify(concertRepository, never()).searchConcerts(any(), any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void should_pass_authenticated_user_id_as_calendar_user_id_when_in_calendar_true() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findConcerts(anyList(), isNull(), eq(USER_ID), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, true, null, null, PAGEABLE, USER_ID);

        // then
        verify(concertRepository).findConcerts(anyList(), isNull(), eq(USER_ID), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE));
        assertThat(response.content()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // getConcerts — followedOnly
    // -------------------------------------------------------------------------

    @Test
    void should_return_empty_page_when_followed_only_true_and_user_not_authenticated() {
        // given
        // userId == null이면 팔로우 목록 조회 없이 즉시 빈 페이지를 반환한다

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, true, null, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        verify(userFollowArtistRepository, never()).findByUserId(any());
        verify(concertRepository, never()).findConcerts(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void should_return_empty_page_when_followed_only_true_and_user_follows_no_artists() {
        // given
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, true, null, PAGEABLE, USER_ID);

        // then
        assertThat(response.content()).isEmpty();
        verify(concertRepository, never()).findConcerts(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void should_pass_followed_artist_ids_when_followed_only_true_and_user_has_follows() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(concertRepository.findConcerts(anyList(), isNull(), isNull(), eq(List.of(ARTIST_ID)), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, true, null, PAGEABLE, USER_ID);

        // then
        verify(concertRepository).findConcerts(anyList(), isNull(), isNull(), eq(List.of(ARTIST_ID)), eq(false), any(LocalDateTime.class), eq(PAGEABLE));
        assertThat(response.content()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // getConcerts — ticketOpenPending / 필터 조합
    // -------------------------------------------------------------------------

    @Test
    void should_pass_ticket_open_pending_true_to_repository_when_ticket_open_pending_given() {
        // given
        Page<Concert> page = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findConcerts(anyList(), isNull(), isNull(), isNull(), eq(true), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, null, true, PAGEABLE, null);

        // then
        verify(concertRepository).findConcerts(anyList(), isNull(), isNull(), isNull(), eq(true), any(LocalDateTime.class), eq(PAGEABLE));
        assertThat(response.content()).isEmpty();
    }

    @Test
    void should_combine_status_calendar_and_ticket_open_pending_filters_when_given_together() {
        // given
        Page<Concert> page = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findConcerts(anyList(), eq(ConcertStatus.UPCOMING), eq(USER_ID), isNull(), eq(true), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(
                null, ConcertStatus.UPCOMING, true, null, true, PAGEABLE, USER_ID);

        // then
        verify(concertRepository).findConcerts(anyList(), eq(ConcertStatus.UPCOMING), eq(USER_ID), isNull(), eq(true), any(LocalDateTime.class), eq(PAGEABLE));
        assertThat(response.content()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getConcerts — isInCalendar 매핑
    // -------------------------------------------------------------------------

    @Test
    void should_return_is_in_calendar_true_when_authenticated_user_has_concert_in_calendar() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        UserConcertCalendar calendarEntry = UserConcertCalendar.builder()
                .userId(USER_ID)
                .concertId(CONCERT_ID)
                .build();

        given(concertRepository.findConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of(calendarEntry));

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, null, null, PAGEABLE, USER_ID);

        // then
        assertThat(response.content().get(0).isInCalendar()).isTrue();
    }

    @Test
    void should_return_is_in_calendar_false_when_user_id_is_null_in_concert_list() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findConcerts(anyList(), isNull(), isNull(), isNull(), eq(false), any(LocalDateTime.class), eq(PAGEABLE)))
                .willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, null, null, null, null, PAGEABLE, null);

        // then
        assertThat(response.content().get(0).isInCalendar()).isFalse();
    }

    // -------------------------------------------------------------------------
    // getConcert
    // -------------------------------------------------------------------------

    @Test
    void should_return_concert_detail_with_ticket_links_when_concert_exists() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        ConcertBookingLink bookingLink = ConcertBookingLink.builder()
                .id(1L)
                .concertId(CONCERT_ID)
                .name("인터파크")
                .url("https://ticket.interpark.com")
                .build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(bookingLink));
        given(userConcertCalendarRepository.existsByUserIdAndConcertId(USER_ID, CONCERT_ID)).willReturn(false);

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, USER_ID);

        // then
        assertThat(response.id()).isEqualTo(CONCERT_ID);
        assertThat(response.artists()).containsExactly(new ArtistSummary(ARTIST_ID, "YOASOBI", null));
        assertThat(response.posterUrl()).isEqualTo("https://example.com/poster.jpg");
        assertThat(response.ticketLinks()).hasSize(1);
        assertThat(response.ticketLinks().get(0).label()).isEqualTo("인터파크");
        assertThat(response.isInCalendar()).isFalse();
        verify(concertRepository).incrementViewCount(CONCERT_ID);
    }

    @Test
    void should_return_is_in_calendar_true_when_user_has_concert_in_calendar() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(userConcertCalendarRepository.existsByUserIdAndConcertId(USER_ID, CONCERT_ID)).willReturn(true);

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, USER_ID);

        // then
        assertThat(response.isInCalendar()).isTrue();
    }

    @Test
    void should_return_is_in_calendar_false_when_user_id_is_null() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, null);

        // then
        assertThat(response.isInCalendar()).isFalse();
    }

    @Test
    void should_return_empty_poster_urls_when_poster_url_is_null() {
        // given
        Concert concert = Concert.builder()
                .id(CONCERT_ID)
                .kopisId("kopis-1")
                .title("공연")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 6, 3))
                .venueName("올림픽공원")
                .posterUrl(null)
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .build();

        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, null);

        // then
        assertThat(response.imageUrls()).isEmpty();
        assertThat(response.posterUrl()).isNull();
    }

    @Test
    void should_throw_concert_not_found_when_concert_does_not_exist() {
        // given
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> concertService.getConcert(CONCERT_ID, USER_ID))
                .isInstanceOf(ConcertNotFoundException.class)
                .hasMessage(ErrorCode.CONCERT_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_concert_not_found_when_concert_is_excluded() {
        // given
        Concert excludedConcert = buildConcert(CONCERT_ID, ConcertStatus.EXCLUDED);
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(excludedConcert));

        // when & then
        assertThatThrownBy(() -> concertService.getConcert(CONCERT_ID, null))
                .isInstanceOf(ConcertNotFoundException.class)
                .hasMessage(ErrorCode.CONCERT_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // getSetlist
    // -------------------------------------------------------------------------

    @Test
    void should_return_tracks_when_setlist_exists_for_concert() {
        // given
        Setlist setlist = Setlist.builder()
                .id(1L)
                .concertId(CONCERT_ID)
                .setlistFmId("setlist-fm-001")
                .collectedAt(java.time.LocalDateTime.now())
                .build();
        SetlistTrack track1 = SetlistTrack.builder()
                .id(1L).setlistId(1L).position(1).songName("Pale Blue").build();
        SetlistTrack track2 = SetlistTrack.builder()
                .id(2L).setlistId(1L).position(2).songName("KICK BACK").build();

        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertIdOrderByCollectedAtDesc(CONCERT_ID)).willReturn(List.of(setlist));
        given(setlistTrackRepository.findBySetlistIdOrderByPosition(1L)).willReturn(List.of(track1, track2));

        // when
        SetlistResponse result = concertService.getSetlist(CONCERT_ID);

        // then
        assertThat(result.tracks()).hasSize(2);
        assertThat(result.tracks().get(0).order()).isEqualTo(1);
        assertThat(result.tracks().get(0).title()).isEqualTo("Pale Blue");
        assertThat(result.tracks().get(1).title()).isEqualTo("KICK BACK");
    }

    @Test
    void should_return_source_url_when_setlist_has_attribution_url() {
        // given
        String attributionUrl = "https://www.setlist.fm/setlist/yoasobi/2025/kspo-dome-1a2b3c4d.html";
        Setlist setlist = Setlist.builder()
                .id(1L)
                .concertId(CONCERT_ID)
                .setlistFmId("setlist-fm-001")
                .collectedAt(LocalDateTime.now())
                .attributionUrl(attributionUrl)
                .build();

        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertIdOrderByCollectedAtDesc(CONCERT_ID)).willReturn(List.of(setlist));
        given(setlistTrackRepository.findBySetlistIdOrderByPosition(1L)).willReturn(List.of());

        // when
        SetlistResponse result = concertService.getSetlist(CONCERT_ID);

        // then
        assertThat(result.sourceUrl()).isEqualTo(attributionUrl);
    }

    @Test
    void should_return_null_source_url_when_setlist_has_no_attribution_url() {
        // given
        Setlist setlist = Setlist.builder()
                .id(1L)
                .concertId(CONCERT_ID)
                .setlistFmId("setlist-fm-001")
                .collectedAt(LocalDateTime.now())
                .build();

        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertIdOrderByCollectedAtDesc(CONCERT_ID)).willReturn(List.of(setlist));
        given(setlistTrackRepository.findBySetlistIdOrderByPosition(1L)).willReturn(List.of());

        // when
        SetlistResponse result = concertService.getSetlist(CONCERT_ID);

        // then
        assertThat(result.sourceUrl()).isNull();
    }

    @Test
    void should_return_empty_tracks_when_no_setlist_exists_for_concert() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertIdOrderByCollectedAtDesc(CONCERT_ID)).willReturn(List.of());

        // when
        SetlistResponse result = concertService.getSetlist(CONCERT_ID);

        // then
        assertThat(result.tracks()).isEmpty();
        assertThat(result.sourceUrl()).isNull();
    }

    @Test
    void should_throw_concert_not_found_when_concert_does_not_exist_for_setlist() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> concertService.getSetlist(CONCERT_ID))
                .isInstanceOf(ConcertNotFoundException.class)
                .hasMessage(ErrorCode.CONCERT_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // ticketOpenAt 매핑
    // -------------------------------------------------------------------------

    @Test
    void should_include_ticket_open_at_in_summary_when_concert_has_ticket_open_at() {
        // given
        LocalDateTime ticketOpenAt = LocalDateTime.of(2025, 5, 1, 10, 0);
        Concert concert = Concert.builder()
                .id(CONCERT_ID)
                .kopisId("kopis-1")
                .title("공연")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 6, 3))
                .venueName("올림픽공원")
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .ticketOpenAt(ticketOpenAt)
                .build();
        given(concertRepository.findTop10Popular(anyList())).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result.get(0).ticketOpenAt()).isEqualTo(ticketOpenAt);
    }

    @Test
    void should_include_ticket_open_at_in_detail_when_concert_has_ticket_open_at() {
        // given
        LocalDateTime ticketOpenAt = LocalDateTime.of(2025, 5, 1, 10, 0);
        Concert concert = Concert.builder()
                .id(CONCERT_ID)
                .kopisId("kopis-1")
                .title("공연")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 6, 3))
                .venueName("올림픽공원")
                .status(ConcertStatus.UPCOMING)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .ticketOpenAt(ticketOpenAt)
                .build();
        given(concertRepository.findById(CONCERT_ID)).willReturn(Optional.of(concert));
        given(concertArtistRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, null);

        // then
        assertThat(response.ticketOpenAt()).isEqualTo(ticketOpenAt);
    }

    // -------------------------------------------------------------------------
    // getTicketingConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_upcoming_ticketing_concerts_ordered_by_ticket_open_at() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findUpcomingTicketing(any(LocalDateTime.class), anyList(), any(Pageable.class)))
                .willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getTicketingConcerts(null, false);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(CONCERT_ID);
    }

    @Test
    void should_return_following_ticketing_concerts_when_following_is_true() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(concertRepository.findUpcomingTicketingByArtistIds(any(LocalDateTime.class), anyList(), eq(List.of(ARTIST_ID)), any(Pageable.class)))
                .willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getTicketingConcerts(USER_ID, true);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(CONCERT_ID);
    }

    @Test
    void should_return_empty_list_when_following_is_true_and_user_follows_no_artists() {
        // given
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getTicketingConcerts(USER_ID, true);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_query_with_page_size_20_when_getting_ticketing_concerts() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findUpcomingTicketing(any(LocalDateTime.class), anyList(), any(Pageable.class)))
                .willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(any())).willReturn(List.of());

        // when
        concertService.getTicketingConcerts(null, false);

        // then
        verify(concertRepository).findUpcomingTicketing(any(LocalDateTime.class), anyList(), eq(PageRequest.of(0, 20)));
    }
}
