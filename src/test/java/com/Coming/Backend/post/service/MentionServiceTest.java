package com.Coming.Backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
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
        PageResponse<EntityCardResponse> result = mentionService.search(List.of(EntityType.CONCERT), q, PAGE, SIZE);

        // then
        verify(concertRepository).searchByTitleForMention(any(), eq("%아이유%"), any(Pageable.class));
        assertThat(result.content()).containsExactly(card);
        assertThat(result.page()).isEqualTo(PAGE);
        assertThat(result.size()).isEqualTo(SIZE);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void should_escape_like_wildcards_when_type_is_ARTIST_and_query_contains_wildcards() {
        // given
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(eq("50\\%\\_a"), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        mentionService.search(List.of(EntityType.ARTIST), "50%_a", PAGE, SIZE);

        // then
        verify(artistRepository).findByNameOrAliasContainingIgnoreCase(eq("50\\%\\_a"), any(Pageable.class));
    }

    @Test
    void should_return_artist_cards_when_type_is_ARTIST() {
        // given
        String q = "아이유";
        Artist artist = Artist.builder().id(ARTIST_ID).name("아이유").build();
        Page<Artist> page = new PageImpl<>(List.of(artist));
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, ARTIST_ID, "아이유", null, null, null);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(eq(q), any(Pageable.class))).willReturn(page);
        given(entityLookupService.toCard(artist)).willReturn(card);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(List.of(EntityType.ARTIST), q, PAGE, SIZE);

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
        PageResponse<EntityCardResponse> result = mentionService.search(List.of(EntityType.RELEASE), q, PAGE, SIZE);

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
        PageResponse<EntityCardResponse> result = mentionService.search(List.of(EntityType.TRACK), q, PAGE, SIZE);

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
        PageResponse<EntityCardResponse> result = mentionService.search(List.of(EntityType.CONCERT), "없는공연", PAGE, SIZE);

        // then
        assertThat(result.content()).isEmpty();
    }

    @Test
    void should_escape_like_wildcards_when_query_contains_percent_and_underscore() {
        // given
        given(concertRepository.searchByTitleForMention(any(), eq("%50\\%\\_off%"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        mentionService.search(List.of(EntityType.CONCERT), "50%_off", PAGE, SIZE);

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
        mentionService.search(List.of(EntityType.CONCERT), "아이유", requestedPage, requestedSize);

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
        mentionService.search(List.of(EntityType.ARTIST), "아이유", PAGE, SIZE);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(artistRepository).findByNameOrAliasContainingIgnoreCase(any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void should_place_prefix_matched_card_first_when_multiple_types_given() {
        // given
        Concert concert = Concert.builder().id(CONCERT_ID).title("공연 아이유").build();
        Artist artist = Artist.builder().id(ARTIST_ID).name("아이유").build();
        EntityCardResponse concertCard = new EntityCardResponse(EntityType.CONCERT, CONCERT_ID, "공연 아이유", null, null, null);
        EntityCardResponse artistCard = new EntityCardResponse(EntityType.ARTIST, ARTIST_ID, "아이유", null, null, null);
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(concert)));
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(artist)));
        given(entityLookupService.toCard(concert)).willReturn(concertCard);
        given(entityLookupService.toCard(artist)).willReturn(artistCard);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(
                List.of(EntityType.CONCERT, EntityType.ARTIST), "아이유", PAGE, SIZE);

        // then
        assertThat(result.content()).containsExactly(artistCard, concertCard);
    }

    @Test
    void should_keep_type_order_when_prefix_match_is_equal() {
        // given
        Concert concert = Concert.builder().id(CONCERT_ID).title("아이유 콘서트").build();
        Artist artist = Artist.builder().id(ARTIST_ID).name("아이유").build();
        EntityCardResponse concertCard = new EntityCardResponse(EntityType.CONCERT, CONCERT_ID, "아이유 콘서트", null, null, null);
        EntityCardResponse artistCard = new EntityCardResponse(EntityType.ARTIST, ARTIST_ID, "아이유", null, null, null);
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(concert)));
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(artist)));
        given(entityLookupService.toCard(concert)).willReturn(concertCard);
        given(entityLookupService.toCard(artist)).willReturn(artistCard);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(
                List.of(EntityType.ARTIST, EntityType.CONCERT), "아이유", PAGE, SIZE);

        // then
        assertThat(result.content()).containsExactly(concertCard, artistCard);
    }

    @Test
    void should_truncate_to_size_when_merged_results_exceed_size() {
        // given
        int size = 2;
        Concert firstConcert = Concert.builder().id(CONCERT_ID).title("아이유 콘서트").build();
        Concert secondConcert = Concert.builder().id(2L).title("아이유 팬미팅").build();
        Artist artist = Artist.builder().id(ARTIST_ID).name("아이유").build();
        EntityCardResponse firstCard = new EntityCardResponse(EntityType.CONCERT, CONCERT_ID, "아이유 콘서트", null, null, null);
        EntityCardResponse secondCard = new EntityCardResponse(EntityType.CONCERT, 2L, "아이유 팬미팅", null, null, null);
        EntityCardResponse artistCard = new EntityCardResponse(EntityType.ARTIST, ARTIST_ID, "아이유", null, null, null);
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(firstConcert, secondConcert)));
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(artist)));
        given(entityLookupService.toCard(firstConcert)).willReturn(firstCard);
        given(entityLookupService.toCard(secondConcert)).willReturn(secondCard);
        given(entityLookupService.toCard(artist)).willReturn(artistCard);

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(
                List.of(EntityType.CONCERT, EntityType.ARTIST), "아이유", PAGE, size);

        // then
        assertThat(result.content()).containsExactly(firstCard, secondCard);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void should_search_all_four_types_with_page_zero_when_types_is_null() {
        // given
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class))).willReturn(Page.empty());
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(any(), any(Pageable.class))).willReturn(Page.empty());
        given(releaseGroupRepository.searchByTitleForMention(any(), any(Pageable.class))).willReturn(Page.empty());
        given(trackRepository.searchByTitleForMention(any(), any(Pageable.class))).willReturn(Page.empty());

        // when
        mentionService.search(null, "아이유", PAGE, SIZE);

        // then
        verify(concertRepository).searchByTitleForMention(any(), any(), eq(PageRequest.of(0, SIZE)));
        verify(artistRepository).findByNameOrAliasContainingIgnoreCase(
                any(), eq(PageRequest.of(0, SIZE, Sort.by(Sort.Direction.ASC, "id"))));
        verify(releaseGroupRepository).searchByTitleForMention(any(), eq(PageRequest.of(0, SIZE)));
        verify(trackRepository).searchByTitleForMention(any(), eq(PageRequest.of(0, SIZE)));
    }

    @Test
    void should_throw_invalid_input_exception_when_multiple_types_and_page_is_not_zero() {
        // when & then
        assertThatThrownBy(() -> mentionService.search(
                List.of(EntityType.CONCERT, EntityType.ARTIST), "아이유", 1, SIZE))
                .isInstanceOf(InvalidInputException.class);
        verifyNoInteractions(concertRepository, artistRepository, releaseGroupRepository, trackRepository);
    }

    @Test
    void should_return_empty_content_with_zero_total_pages_when_multiple_types_have_no_result() {
        // given
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class))).willReturn(Page.empty());
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(any(), any(Pageable.class))).willReturn(Page.empty());

        // when
        PageResponse<EntityCardResponse> result = mentionService.search(
                List.of(EntityType.CONCERT, EntityType.ARTIST), "없는검색어", PAGE, SIZE);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void should_pass_requested_page_when_duplicated_single_type_given() {
        // given
        int requestedPage = 2;
        given(concertRepository.searchByTitleForMention(any(), any(), any(Pageable.class))).willReturn(Page.empty());

        // when
        mentionService.search(List.of(EntityType.CONCERT, EntityType.CONCERT), "아이유", requestedPage, SIZE);

        // then
        verify(concertRepository).searchByTitleForMention(any(), any(), eq(PageRequest.of(requestedPage, SIZE)));
    }
}
