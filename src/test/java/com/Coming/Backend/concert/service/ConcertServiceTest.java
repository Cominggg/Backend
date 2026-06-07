package com.Coming.Backend.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.calendar.entity.UserConcertCalendar;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.response.PageResponse;
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

        given(concertRepository.findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED)).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).artistName()).isEqualTo("YOASOBI");
        verify(concertRepository).findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED);
    }

    @Test
    void should_return_empty_list_when_no_popular_concerts_exist() {
        // given
        given(concertRepository.findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED)).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_is_in_calendar_false_for_popular_concerts_when_user_is_not_authenticated() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);

        given(concertRepository.findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED)).willReturn(List.of(concert));
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

        given(concertRepository.findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED)).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of(calendarEntry));

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(USER_ID);

        // then
        assertThat(result.get(0).isInCalendar()).isTrue();
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
    // getConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_concerts_with_artist_name_when_no_status_filter_applied() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");

        given(concertRepository.findByStatusNot(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, PAGEABLE, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).artistName()).isEqualTo("YOASOBI");
        assertThat(response.content().get(0).venue()).isEqualTo("올림픽공원");
        assertThat(response.content().get(0).status()).isEqualTo(ConcertStatus.UPCOMING);
    }

    @Test
    void should_return_concerts_with_null_artist_name_when_no_artist_mapped() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByStatusNot(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, PAGEABLE, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).artistName()).isNull();
    }

    @Test
    void should_pass_status_filter_to_repository_when_status_is_given() {
        // given
        Page<Concert> page = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findByStatus(ConcertStatus.UPCOMING, PAGEABLE)).willReturn(page);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(ConcertStatus.UPCOMING, PAGEABLE, null);

        // then
        verify(concertRepository).findByStatus(ConcertStatus.UPCOMING, PAGEABLE);
        assertThat(response.content()).isEmpty();
    }

    @Test
    void should_return_empty_page_when_no_concerts_match_filter() {
        // given
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);
        given(concertRepository.findByStatusNot(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(emptyPage);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
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
        given(concertArtistRepository.findFirstByConcertIdOrderByIdAsc(CONCERT_ID))
                .willReturn(Optional.of(concertArtist));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of(bookingLink));
        given(userConcertCalendarRepository.existsByUserIdAndConcertId(USER_ID, CONCERT_ID)).willReturn(false);

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, USER_ID);

        // then
        assertThat(response.id()).isEqualTo(CONCERT_ID);
        assertThat(response.artistName()).isEqualTo("YOASOBI");
        assertThat(response.artistId()).isEqualTo(ARTIST_ID);
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
        given(concertArtistRepository.findFirstByConcertIdOrderByIdAsc(CONCERT_ID))
                .willReturn(Optional.empty());
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
        given(concertArtistRepository.findFirstByConcertIdOrderByIdAsc(CONCERT_ID))
                .willReturn(Optional.empty());
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
        given(concertArtistRepository.findFirstByConcertIdOrderByIdAsc(CONCERT_ID))
                .willReturn(Optional.empty());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, null);

        // then
        assertThat(response.imageUrls()).isEmpty();
        assertThat(response.posterUrl()).isNull();
    }

    // -------------------------------------------------------------------------
    // getFollowingConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_empty_list_when_user_follows_no_artists() {
        // given
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getFollowingConcerts(USER_ID, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_all_concerts_when_status_is_null() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");

        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(concertRepository.findAllByArtistIdIn(List.of(ARTIST_ID), ConcertStatus.EXCLUDED)).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getFollowingConcerts(USER_ID, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).artistName()).isEqualTo("YOASOBI");
        assertThat(result.get(0).status()).isEqualTo(ConcertStatus.UPCOMING);
    }

    @Test
    void should_return_filtered_concerts_when_status_is_given() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();

        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(concertRepository.findAllByArtistIdInAndStatus(List.of(ARTIST_ID), ConcertStatus.ONGOING))
                .willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getFollowingConcerts(USER_ID, ConcertStatus.ONGOING);

        // then
        verify(concertRepository).findAllByArtistIdInAndStatus(List.of(ARTIST_ID), ConcertStatus.ONGOING);
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_null_artist_name_when_no_artist_mapped_in_following_concerts() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);

        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(concertRepository.findAllByArtistIdIn(List.of(ARTIST_ID), ConcertStatus.EXCLUDED)).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getFollowingConcerts(USER_ID, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).artistName()).isNull();
    }

    // -------------------------------------------------------------------------
    // getConcerts — isInCalendar
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

        given(concertRepository.findByStatusNot(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of(calendarEntry));

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, PAGEABLE, USER_ID);

        // then
        assertThat(response.content().get(0).isInCalendar()).isTrue();
    }

    @Test
    void should_return_is_in_calendar_false_when_user_id_is_null_in_concert_list() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);

        given(concertRepository.findByStatusNot(ConcertStatus.EXCLUDED, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(null, PAGEABLE, null);

        // then
        assertThat(response.content().get(0).isInCalendar()).isFalse();
    }

    // -------------------------------------------------------------------------
    // getFollowingConcerts — isInCalendar
    // -------------------------------------------------------------------------

    @Test
    void should_return_is_in_calendar_true_when_following_concert_is_in_calendar() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        UserConcertCalendar calendarEntry = UserConcertCalendar.builder()
                .userId(USER_ID)
                .concertId(CONCERT_ID)
                .build();

        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(concertRepository.findAllByArtistIdIn(List.of(ARTIST_ID), ConcertStatus.EXCLUDED)).willReturn(List.of(concert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID)))
                .willReturn(List.of());
        given(userConcertCalendarRepository.findByUserIdAndConcertIdIn(USER_ID, List.of(CONCERT_ID)))
                .willReturn(List.of(calendarEntry));

        // when
        List<ConcertSummaryResponse> result = concertService.getFollowingConcerts(USER_ID, null);

        // then
        assertThat(result.get(0).isInCalendar()).isTrue();
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
    void should_return_empty_tracks_when_no_setlist_exists_for_concert() {
        // given
        given(concertRepository.existsById(CONCERT_ID)).willReturn(true);
        given(setlistRepository.findByConcertIdOrderByCollectedAtDesc(CONCERT_ID)).willReturn(List.of());

        // when
        SetlistResponse result = concertService.getSetlist(CONCERT_ID);

        // then
        assertThat(result.tracks()).isEmpty();
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
    // EXCLUDED 필터링
    // -------------------------------------------------------------------------

    @Test
    void should_return_empty_page_when_status_is_excluded() {
        // given
        // status == EXCLUDED이면 레포를 호출하지 않고 즉시 빈 페이지를 반환한다

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(ConcertStatus.EXCLUDED, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(concertRepository, never()).findByStatus(any(), any());
        verify(concertRepository, never()).findByStatusNot(any(), any());
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

    @Test
    void should_return_empty_list_when_following_concerts_status_is_excluded() {
        // given
        // status == EXCLUDED이면 레포를 호출하지 않고 즉시 빈 리스트를 반환한다

        // when
        List<ConcertSummaryResponse> result = concertService.getFollowingConcerts(USER_ID, ConcertStatus.EXCLUDED);

        // then
        assertThat(result).isEmpty();
        verify(userFollowArtistRepository, never()).findByUserId(any());
    }

    @Test
    void should_not_include_excluded_concerts_in_popular_list() {
        // given
        Concert upcomingConcert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        given(concertRepository.findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED))
                .willReturn(List.of(upcomingConcert));
        given(concertArtistRepository.findByConcertIdIn(List.of(CONCERT_ID))).willReturn(List.of());

        // when
        List<ConcertSummaryResponse> result = concertService.getPopularConcerts(null);

        // then
        verify(concertRepository).findTop10ByStatusNotOrderByViewCountDesc(ConcertStatus.EXCLUDED);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ConcertStatus.UPCOMING);
    }
}
