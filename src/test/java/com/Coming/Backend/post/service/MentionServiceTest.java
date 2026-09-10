package com.Coming.Backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    private EntityLookupService entityLookupService;

    private static final int LIMIT = 10;
    private static final Long CONCERT_ID = 1L;
    private static final Long ARTIST_ID = 10L;
    private static final Long RELEASE_ID = 100L;
    private static final Long RELEASE_ARTIST_ID = 20L;

    @Test
    void should_return_concert_cards_when_type_is_CONCERT() {
        // given
        String q = "아이유";
        Concert concert = Concert.builder().id(CONCERT_ID).title("아이유 콘서트").build();
        EntityCardResponse card = new EntityCardResponse(EntityType.CONCERT, CONCERT_ID, "아이유 콘서트", null, null);
        given(concertRepository.searchByTitleForMention(any(), eq("%아이유%"), any(Pageable.class)))
                .willReturn(List.of(concert));
        given(entityLookupService.toCard(concert)).willReturn(card);

        // when
        List<EntityCardResponse> result = mentionService.search(EntityType.CONCERT, q, LIMIT);

        // then
        verify(concertRepository).searchByTitleForMention(any(), eq("%아이유%"), any(Pageable.class));
        assertThat(result).containsExactly(card);
    }

    @Test
    void should_call_artist_repository_with_raw_query_when_type_is_ARTIST() {
        // given
        String q = "아이유";
        Artist artist = Artist.builder().id(ARTIST_ID).name("아이유").build();
        Page<Artist> page = new PageImpl<>(List.of(artist));
        EntityCardResponse card = new EntityCardResponse(EntityType.ARTIST, ARTIST_ID, "아이유", null, null);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase(eq(q), any(Pageable.class))).willReturn(page);
        given(entityLookupService.toCard(artist)).willReturn(card);

        // when
        List<EntityCardResponse> result = mentionService.search(EntityType.ARTIST, q, LIMIT);

        // then
        verify(artistRepository).findByNameOrAliasContainingIgnoreCase(eq("아이유"), any(Pageable.class));
        assertThat(result).containsExactly(card);
    }

    @Test
    void should_map_artist_names_by_release_artist_id_when_type_is_RELEASE() {
        // given
        String q = "LILAC";
        ReleaseGroup release = ReleaseGroup.builder().id(RELEASE_ID).artistId(RELEASE_ARTIST_ID).title("LILAC").build();
        Artist releaseArtist = Artist.builder().id(RELEASE_ARTIST_ID).name("IU").build();
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release));
        EntityCardResponse card = new EntityCardResponse(EntityType.RELEASE, RELEASE_ID, "LILAC", "IU", null);
        given(releaseGroupRepository.searchReleases(eq(null), eq(null), eq(null), eq("%lilac%"), any(Pageable.class)))
                .willReturn(page);
        given(artistRepository.findAllById(eq(Set.of(RELEASE_ARTIST_ID)))).willReturn(List.of(releaseArtist));
        given(entityLookupService.toCard(release, "IU")).willReturn(card);

        // when
        List<EntityCardResponse> result = mentionService.search(EntityType.RELEASE, q, LIMIT);

        // then
        verify(artistRepository).findAllById(eq(Set.of(RELEASE_ARTIST_ID)));
        assertThat(result).containsExactly(card);
    }

    @Test
    void should_return_empty_list_when_no_search_result_found() {
        // given
        given(concertRepository.searchByTitleForMention(any(), eq("%없는공연%"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        List<EntityCardResponse> result = mentionService.search(EntityType.CONCERT, "없는공연", LIMIT);

        // then
        assertThat(result).isEmpty();
    }
}
