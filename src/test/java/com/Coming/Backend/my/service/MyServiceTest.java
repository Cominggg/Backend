package com.Coming.Backend.my.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertArtist;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertArtistRepository;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.my.dto.ConcertHistoryResponse;

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

@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @InjectMocks
    private MyService myService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ConcertArtistRepository concertArtistRepository;

    @Mock
    private ArtistRepository artistRepository;

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
        PageResponse<ConcertHistoryResponse> result = myService.getHistory(USER_ID, PAGEABLE);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(CONCERT_ID);
        assertThat(result.content().get(0).artistName()).isEqualTo("YOASOBI");
        assertThat(result.content().get(0).title()).isEqualTo("공연 1");
        assertThat(result.content().get(0).venue()).isEqualTo("KSPO DOME, 서울");
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
        PageResponse<ConcertHistoryResponse> result = myService.getHistory(USER_ID, PAGEABLE);

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
        PageResponse<ConcertHistoryResponse> result = myService.getHistory(USER_ID, PAGEABLE);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).artistName()).isNull();
    }
}
