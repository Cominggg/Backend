package com.Coming.Backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.entity.Track;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import com.Coming.Backend.release.repository.TrackRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    @InjectMocks
    private MentionService mentionService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ReleaseGroupRepository releaseGroupRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private EntityLookupService entityLookupService;

    private static final int PAGE = 0;
    private static final int SIZE = 10;
    private static final Long CONCERT_ID = 1L;
    private static final Long ARTIST_ID = 10L;
    private static final Long RELEASE_ID = 100L;
    private static final Long RELEASE_ARTIST_ID = 20L;
    private static final Long TRACK_ID = 200L;

    @Test
    void should_return_concert_cards_when_type_is_CONCERT() {
        // given
        String q = "아이유";
        Concert concert = Concert.builder().id(CONCERT_ID).title("아이유 콘서트").build();
        EntityCardResponse card = new EntityCardResponse(EntityType.CONCERT, CONCERT_ID, "아이유 콘서트", null, null, null);
        Page<Concert> concertPage = new PageImpl<>(List.of(concert), PageRequest.of(PAGE, SIZE), 1);
        given(concertRepository.searchByTitleForMention(any(), eq("%아이유%"), any(Pageable.class)))
                .willReturn(concertPage);
        given(entityLookupService.toCard(concert)).willReturn(card);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(EntityType.CONCERT, q, PAGE, SIZE);

        // then
        verify(concertRepository).searchByTitleForMention(any(), eq("%아이유%"), any(Pageable.class));
        assertThat(result.content()).containsExactly(card);
        assertThat(result.page()).isEqualTo(PAGE);
        assertThat(result.size()).isEqualTo(SIZE);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void should_call_artist_repository_with_raw_query_when_type_is_ARTIST() {
        // given
        String q = "아이유";
        Artist artist = Artist.builder().id(ARTIST_ID).name("아이유").build();
        Page<Artist> page = new PageImpl<>(List.of(artist));
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, ARTIST_ID, "아이유", null, null, null);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(eq(q), any(Pageable.class))).willReturn(page);
        given(entityLookupService.toCard(artist)).willReturn(card);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(EntityType.ARTIST, q, PAGE, SIZE);

        // then
        verify(artistRepository).findByNameOrAliasContainingIgnoreCase(eq("아이유"), any(Pageable.class));
        assertThat(result.content()).containsExactly(card);
    }

    @Test
    void should_map_artist_names_by_release_artist_id_when_type_is_RELEASE() {
        // given
        String q = "LILAC";
        ReleaseGroup release = ReleaseGroup.builder().id(RELEASE_ID).artistId(RELEASE_ARTIST_ID).title("LILAC").build();
        Artist releaseArtist = Artist.builder().id(RELEASE_ARTIST_ID).name("IU").build();
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release));
        EntityCardResponse card = new EntityCardResponse(EntityType.RELEASE, RELEASE_ID, "LILAC", "IU", null, null);
        given(releaseGroupRepository.searchByTitleForMention(eq("%lilac%"), any(Pageable.class)))
                .willReturn(page);
        given(artistRepository.findAllById(eq(Set.of(RELEASE_ARTIST_ID)))).willReturn(List.of(releaseArtist));
        given(entityLookupService.toCard(release, "IU")).willReturn(card);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(EntityType.RELEASE, q, PAGE, SIZE);

        // then
        verify(artistRepository).findAllById(eq(Set.of(RELEASE_ARTIST_ID)));
        assertThat(result.content()).containsExactly(card);
    }

    @Test
    void should_return_track_cards_when_type_is_TRACK() {
        // given
        String q = "라일락";
        Track track = Track.builder().id(TRACK_ID).releaseGroupId(RELEASE_ID).title("라일락").position(1).build();
        Page<Track> trackPage = new PageImpl<>(List.of(track));
        EntityCardResponse card = new EntityCardResponse(EntityType.TRACK, TRACK_ID, "라일락", "IU · LILAC", null, RELEASE_ID);
        given(trackRepository.searchByTitleForMention(eq("%라일락%"), any(Pageable.class))).willReturn(trackPage);
        given(entityLookupService.toTrackCardsById(List.of(track))).willReturn(Map.of(TRACK_ID, card));

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(EntityType.TRACK, q, PAGE, SIZE);

        // then
        verify(trackRepository).searchByTitleForMention(eq("%라일락%"), any(Pageable.class));
        assertThat(result.content()).containsExactly(card);
    }

    @Test
    void should_return_empty_list_when_no_search_result_found() {
        // given
        given(concertRepository.searchByTitleForMention(any(), eq("%없는공연%"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(EntityType.CONCERT, "없는공연", PAGE, SIZE);

        // then
        assertThat(result.content()).isEmpty();
    }

    @Test
    void should_escape_like_wildcards_when_query_contains_percent_and_underscore() {
        // given
        given(concertRepository.searchByTitleForMention(any(), eq("%50\\%\\_off%"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        mentionService.search(EntityType.CONCERT, "50%_off", PAGE, SIZE);

        // then
        verify(concertRepository).searchByTitleForMention(any(), eq("%50\\%\\_off%"), any(Pageable.class));
    }

    @Test
    void should_build_pageable_with_requested_page_number_when_page_is_not_zero() {
        // given
        int requestedPage = 1;
        int requestedSize = 5;
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        mentionService.search(EntityType.CONCERT, "아이유", requestedPage, requestedSize);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(concertRepository).searchByTitleForMention(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(requestedPage);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(requestedSize);
    }

    @Test
    void should_request_id_ascending_sort_when_type_is_ARTIST() {
        // given
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        mentionService.search(EntityType.ARTIST, "아이유", PAGE, SIZE);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(artistRepository).findByNameOrAliasContainingIgnoreCase(any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }
}
