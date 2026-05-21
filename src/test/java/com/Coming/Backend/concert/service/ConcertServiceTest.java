package com.Coming.Backend.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.dto.ConcertDetailResponse;
import com.Coming.Backend.concert.dto.ConcertFilterRequest;
import com.Coming.Backend.concert.dto.ConcertSummaryResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertBookingLink;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertBookingLinkRepository;
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
    private ArtistRepository artistRepository;

    @Mock
    private UserConcertCalendarRepository userConcertCalendarRepository;

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
                .confidence("HIGH")
                .matchedBy("manual")
                .build();
    }

    // -------------------------------------------------------------------------
    // getConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_concerts_with_artist_name_when_no_filter_applied() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        ConcertArtist concertArtist = buildConcertArtist(CONCERT_ID, ARTIST_ID);
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        ConcertFilterRequest filter = new ConcertFilterRequest(null, null, null, null);

        given(concertRepository.findConcerts(null, null, null, null, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdInAndConfidence(List.of(CONCERT_ID), "HIGH"))
                .willReturn(List.of(concertArtist));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(filter, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).artistName()).isEqualTo("YOASOBI");
        assertThat(response.content().get(0).venue()).isEqualTo("올림픽공원");
        assertThat(response.content().get(0).status()).isEqualTo(ConcertStatus.UPCOMING);
    }

    @Test
    void should_return_concerts_with_null_artist_name_when_no_high_confidence_artist() {
        // given
        Concert concert = buildConcert(CONCERT_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        ConcertFilterRequest filter = new ConcertFilterRequest(null, null, null, null);

        given(concertRepository.findConcerts(null, null, null, null, PAGEABLE)).willReturn(page);
        given(concertArtistRepository.findByConcertIdInAndConfidence(List.of(CONCERT_ID), "HIGH"))
                .willReturn(List.of());

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(filter, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).artistName()).isNull();
    }

    @Test
    void should_pass_filter_params_to_repository_when_filter_is_applied() {
        // given
        LocalDate dateFrom = LocalDate.of(2025, 1, 1);
        LocalDate dateTo = LocalDate.of(2025, 12, 31);
        ConcertFilterRequest filter = new ConcertFilterRequest(dateFrom, dateTo, ARTIST_ID, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(), PAGEABLE, 0);

        given(concertRepository.findConcerts(dateFrom, dateTo, ARTIST_ID, ConcertStatus.UPCOMING, PAGEABLE))
                .willReturn(page);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(filter, PAGEABLE);

        // then
        verify(concertRepository).findConcerts(dateFrom, dateTo, ARTIST_ID, ConcertStatus.UPCOMING, PAGEABLE);
        assertThat(response.content()).isEmpty();
    }

    @Test
    void should_return_empty_page_when_no_concerts_match_filter() {
        // given
        ConcertFilterRequest filter = new ConcertFilterRequest(null, null, null, null);
        Page<Concert> emptyPage = new PageImpl<>(List.of(), PAGEABLE, 0);

        given(concertRepository.findConcerts(null, null, null, null, PAGEABLE)).willReturn(emptyPage);

        // when
        PageResponse<ConcertSummaryResponse> response = concertService.getConcerts(filter, PAGEABLE);

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
        given(concertArtistRepository.findFirstByConcertIdAndConfidence(CONCERT_ID, "HIGH"))
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
        assertThat(response.posterUrls()).containsExactly("https://example.com/poster.jpg");
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
        given(concertArtistRepository.findFirstByConcertIdAndConfidence(CONCERT_ID, "HIGH"))
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
        given(concertArtistRepository.findFirstByConcertIdAndConfidence(CONCERT_ID, "HIGH"))
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
        given(concertArtistRepository.findFirstByConcertIdAndConfidence(CONCERT_ID, "HIGH"))
                .willReturn(Optional.empty());
        given(concertBookingLinkRepository.findByConcertId(CONCERT_ID)).willReturn(List.of());

        // when
        ConcertDetailResponse response = concertService.getConcert(CONCERT_ID, null);

        // then
        assertThat(response.posterUrls()).isEmpty();
        assertThat(response.thumbnailUrl()).isNull();
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
}
