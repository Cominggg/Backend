package com.Coming.Backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.service.EntityLookupService.EntityKey;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.entity.Track;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import com.Coming.Backend.release.repository.TrackRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityLookupServiceTest {

    @InjectMocks
    private EntityLookupService entityLookupService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ReleaseGroupRepository releaseGroupRepository;

    @Mock
    private TrackRepository trackRepository;

    private static final Long CONCERT_ID = 1L;
    private static final Long ARTIST_ID = 10L;
    private static final Long RELEASE_ID = 100L;
    private static final Long RELEASE_ARTIST_ID = 20L;
    private static final Long TRACK_ID = 200L;

    private Concert buildConcert(Long id) {
        return Concert.builder()
                .id(id)
                .title("IU Concert : HEREH WORLD TOUR")
                .startDate(LocalDate.of(2025, 10, 1))
                .venueName("올림픽공원 케이스포돔")
                .posterUrl("https://example.com/concert-poster.jpg")
                .build();
    }

    private Artist buildArtist(Long id, String name) {
        return Artist.builder()
                .id(id)
                .mbid("mbid-" + id)
                .name(name)
                .imageUrl("https://example.com/artist-" + id + ".jpg")
                .build();
    }

    private ReleaseGroup buildReleaseGroup(Long id, Long artistId) {
        return ReleaseGroup.builder()
                .id(id)
                .artistId(artistId)
                .title("LILAC")
                .coverUrl("https://example.com/release-" + id + ".jpg")
                .build();
    }

    private Track buildTrack(Long id, Long releaseGroupId) {
        return Track.builder()
                .id(id)
                .releaseGroupId(releaseGroupId)
                .title("라일락")
                .position(1)
                .build();
    }

    @Test
    void should_fill_subtitle_with_date_and_venue_and_thumbnail_with_poster_url_when_concert_key_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID);
        given(concertRepository.findAllById(any())).willReturn(List.of(concert));

        // when
        Map<EntityKey, EntityCardResponse> result =
                entityLookupService.findCards(List.of(new EntityKey(EntityType.CONCERT, CONCERT_ID)));

        // then
        EntityCardResponse card = result.get(new EntityKey(EntityType.CONCERT, CONCERT_ID));
        assertThat(card.subtitle()).isEqualTo("2025-10-01 · 올림픽공원 케이스포돔");
        assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/concert-poster.jpg");
    }

    @Test
    void should_return_null_subtitle_and_image_url_as_thumbnail_when_artist_key_given() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        Map<EntityKey, EntityCardResponse> result =
                entityLookupService.findCards(List.of(new EntityKey(EntityType.ARTIST, ARTIST_ID)));

        // then
        EntityCardResponse card = result.get(new EntityKey(EntityType.ARTIST, ARTIST_ID));
        assertThat(card.subtitle()).isNull();
        assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/artist-10.jpg");
    }

    @Test
    void should_fill_subtitle_with_joined_artist_name_and_thumbnail_with_cover_url_when_release_key_given() {
        // given
        ReleaseGroup releaseGroup = buildReleaseGroup(RELEASE_ID, RELEASE_ARTIST_ID);
        Artist artist = buildArtist(RELEASE_ARTIST_ID, "IU");
        given(releaseGroupRepository.findAllById(any())).willReturn(List.of(releaseGroup));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        Map<EntityKey, EntityCardResponse> result =
                entityLookupService.findCards(List.of(new EntityKey(EntityType.RELEASE, RELEASE_ID)));

        // then
        EntityCardResponse card = result.get(new EntityKey(EntityType.RELEASE, RELEASE_ID));
        assertThat(card.subtitle()).isEqualTo("IU");
        assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/release-100.jpg");
    }

    @Test
    void should_fill_subtitle_with_artist_and_album_and_thumbnail_with_cover_url_when_track_key_given() {
        // given
        Track track = buildTrack(TRACK_ID, RELEASE_ID);
        ReleaseGroup releaseGroup = buildReleaseGroup(RELEASE_ID, RELEASE_ARTIST_ID);
        Artist artist = buildArtist(RELEASE_ARTIST_ID, "IU");
        given(trackRepository.findAllById(any())).willReturn(List.of(track));
        given(releaseGroupRepository.findAllById(any())).willReturn(List.of(releaseGroup));
        given(artistRepository.findAllById(any())).willReturn(List.of(artist));

        // when
        Map<EntityKey, EntityCardResponse> result =
                entityLookupService.findCards(List.of(new EntityKey(EntityType.TRACK, TRACK_ID)));

        // then
        EntityCardResponse card = result.get(new EntityKey(EntityType.TRACK, TRACK_ID));
        assertThat(card.subtitle()).isEqualTo("IU · LILAC");
        assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/release-100.jpg");
    }

    @Test
    void should_fill_only_title_when_track_release_group_not_found() {
        // given
        Track track = buildTrack(TRACK_ID, RELEASE_ID);
        given(trackRepository.findAllById(any())).willReturn(List.of(track));
        given(releaseGroupRepository.findAllById(any())).willReturn(List.of());

        // when
        Map<EntityKey, EntityCardResponse> result =
                entityLookupService.findCards(List.of(new EntityKey(EntityType.TRACK, TRACK_ID)));

        // then
        EntityCardResponse card = result.get(new EntityKey(EntityType.TRACK, TRACK_ID));
        assertThat(card.title()).isEqualTo("라일락");
        assertThat(card.subtitle()).isNull();
        assertThat(card.thumbnailUrl()).isNull();
        assertThat(card.releaseGroupId()).isNull();
    }

    @Test
    void should_fill_all_three_types_when_mixed_keys_given() {
        // given
        Concert concert = buildConcert(CONCERT_ID);
        Artist directArtist = buildArtist(ARTIST_ID, "IU");
        ReleaseGroup releaseGroup = buildReleaseGroup(RELEASE_ID, RELEASE_ARTIST_ID);
        Artist releaseArtist = buildArtist(RELEASE_ARTIST_ID, "BTS");

        given(concertRepository.findAllById(Set.of(CONCERT_ID))).willReturn(List.of(concert));
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(directArtist));
        given(releaseGroupRepository.findAllById(Set.of(RELEASE_ID))).willReturn(List.of(releaseGroup));
        given(artistRepository.findAllById(Set.of(RELEASE_ARTIST_ID))).willReturn(List.of(releaseArtist));

        List<EntityKey> keys = List.of(
                new EntityKey(EntityType.CONCERT, CONCERT_ID),
                new EntityKey(EntityType.ARTIST, ARTIST_ID),
                new EntityKey(EntityType.RELEASE, RELEASE_ID)
        );

        // when
        Map<EntityKey, EntityCardResponse> result = entityLookupService.findCards(keys);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(new EntityKey(EntityType.CONCERT, CONCERT_ID)).type()).isEqualTo(EntityType.CONCERT);
        assertThat(result.get(new EntityKey(EntityType.ARTIST, ARTIST_ID)).type()).isEqualTo(EntityType.ARTIST);
        assertThat(result.get(new EntityKey(EntityType.RELEASE, RELEASE_ID)).type()).isEqualTo(EntityType.RELEASE);
        assertThat(result.get(new EntityKey(EntityType.RELEASE, RELEASE_ID)).subtitle()).isEqualTo("BTS");
    }

    @Test
    void should_exclude_key_from_result_when_entity_does_not_exist() {
        // given
        given(concertRepository.findAllById(any())).willReturn(List.of());

        // when
        Map<EntityKey, EntityCardResponse> result =
                entityLookupService.findCards(List.of(new EntityKey(EntityType.CONCERT, 999L)));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_map_when_keys_is_empty() {
        // given
        List<EntityKey> keys = List.of();

        // when
        Map<EntityKey, EntityCardResponse> result = entityLookupService.findCards(keys);

        // then
        assertThat(result).isEmpty();
    }
}
