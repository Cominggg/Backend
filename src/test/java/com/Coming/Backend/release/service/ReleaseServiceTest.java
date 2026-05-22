package com.Coming.Backend.release.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.release.dto.ArtistReleaseItemResponse;
import com.Coming.Backend.release.dto.ReleaseDetailResponse;
import com.Coming.Backend.release.dto.ReleaseListItemResponse;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.entity.Track;
import com.Coming.Backend.release.exception.ReleaseNotFoundException;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import com.Coming.Backend.release.repository.TrackRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
class ReleaseServiceTest {

    @InjectMocks
    private ReleaseService releaseService;

    @Mock
    private ReleaseGroupRepository releaseGroupRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private ArtistRepository artistRepository;

    private static final Long ARTIST_ID = 1L;
    private static final Long RELEASE_ID = 10L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final List<String> STANDARD_TYPES = List.of("ALBUM", "SINGLE", "EP");

    private ReleaseGroup buildRelease(Long id, Long artistId, String type) {
        return ReleaseGroup.builder()
                .id(id)
                .mbid("mbid-release-" + id)
                .artistId(artistId)
                .title("미니앨범 " + id)
                .type(type)
                .firstReleaseDate(LocalDate.of(2024, 3, 15))
                .coverUrl("https://cover.example.com/" + id)
                .label("HYBE")
                .build();
    }

    private Track buildTrack(Long id, Long releaseGroupId, int position) {
        return Track.builder()
                .id(id)
                .releaseGroupId(releaseGroupId)
                .mbid("mbid-track-" + id)
                .title("트랙 " + position)
                .position(position)
                .lengthMs(210000)
                .build();
    }

    private Artist buildArtist(Long id, String name) {
        return Artist.builder()
                .id(id)
                .mbid("mbid-artist-" + id)
                .name(name)
                .isComing(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // getArtistReleases
    // -------------------------------------------------------------------------

    @Test
    void should_return_all_releases_when_artist_exists_and_types_is_empty() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(releaseGroupRepository.findByArtistId(ARTIST_ID, PAGEABLE)).willReturn(page);
        given(trackRepository.findByReleaseGroupIdOrderByPosition(RELEASE_ID)).willReturn(List.of());

        // when
        PageResponse<ArtistReleaseItemResponse> response =
                releaseService.getArtistReleases(ARTIST_ID, List.of(), PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findByArtistId(ARTIST_ID, PAGEABLE);
    }

    @Test
    void should_return_filtered_releases_when_types_given() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        List<String> types = List.of("ALBUM");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(releaseGroupRepository.findByArtistIdAndTypeIn(ARTIST_ID, types, PAGEABLE)).willReturn(page);
        given(trackRepository.findByReleaseGroupIdOrderByPosition(RELEASE_ID)).willReturn(List.of());

        // when
        PageResponse<ArtistReleaseItemResponse> response =
                releaseService.getArtistReleases(ARTIST_ID, types, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findByArtistIdAndTypeIn(ARTIST_ID, types, PAGEABLE);
    }

    @Test
    void should_include_tracks_in_each_release_item() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        Track track1 = buildTrack(1L, RELEASE_ID, 1);
        Track track2 = buildTrack(2L, RELEASE_ID, 2);
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(releaseGroupRepository.findByArtistId(ARTIST_ID, PAGEABLE)).willReturn(page);
        given(trackRepository.findByReleaseGroupIdOrderByPosition(RELEASE_ID))
                .willReturn(List.of(track1, track2));

        // when
        PageResponse<ArtistReleaseItemResponse> response =
                releaseService.getArtistReleases(ARTIST_ID, List.of(), PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).tracks()).hasSize(2);
        assertThat(response.content().get(0).tracks().get(0).position()).isEqualTo(1);
        assertThat(response.content().get(0).tracks().get(1).position()).isEqualTo(2);
    }

    @Test
    void should_throw_artist_not_found_when_artist_does_not_exist() {
        // given
        given(artistRepository.existsById(ARTIST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> releaseService.getArtistReleases(ARTIST_ID, List.of(), PAGEABLE))
                .isInstanceOf(ArtistNotFoundException.class)
                .hasMessage(ErrorCode.ARTIST_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // getReleases
    // -------------------------------------------------------------------------

    @Test
    void should_return_all_releases_when_no_filters() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(releaseGroupRepository.findAll(PAGEABLE)).willReturn(page);
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<ReleaseListItemResponse> response =
                releaseService.getReleases(null, null, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findAll(PAGEABLE);
    }

    @Test
    void should_return_releases_filtered_by_artist_id_when_only_artist_id_given() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "SINGLE");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(releaseGroupRepository.findByArtistId(ARTIST_ID, PAGEABLE)).willReturn(page);
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<ReleaseListItemResponse> response =
                releaseService.getReleases(ARTIST_ID, null, PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findByArtistId(ARTIST_ID, PAGEABLE);
    }

    @Test
    void should_return_releases_filtered_by_type_when_only_type_given() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(releaseGroupRepository.findByType("ALBUM", PAGEABLE)).willReturn(page);
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<ReleaseListItemResponse> response =
                releaseService.getReleases(null, "ALBUM", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findByType("ALBUM", PAGEABLE);
    }

    @Test
    void should_return_releases_excluding_standard_types_when_type_is_other() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "LIVE");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(releaseGroupRepository.findByTypeNotIn(STANDARD_TYPES, PAGEABLE)).willReturn(page);
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<ReleaseListItemResponse> response =
                releaseService.getReleases(null, "기타", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findByTypeNotIn(STANDARD_TYPES, PAGEABLE);
    }

    @Test
    void should_return_releases_filtered_by_artist_id_and_type() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "EP");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(releaseGroupRepository.findByArtistIdAndType(ARTIST_ID, "EP", PAGEABLE)).willReturn(page);
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<ReleaseListItemResponse> response =
                releaseService.getReleases(ARTIST_ID, "EP", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        verify(releaseGroupRepository).findByArtistIdAndType(ARTIST_ID, "EP", PAGEABLE);
    }

    @Test
    void should_include_artist_name_in_response() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<ReleaseGroup> page = new PageImpl<>(List.of(release), PAGEABLE, 1);
        given(releaseGroupRepository.findAll(PAGEABLE)).willReturn(page);
        given(artistRepository.findAllById(Set.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        PageResponse<ReleaseListItemResponse> response =
                releaseService.getReleases(null, null, PAGEABLE);

        // then
        assertThat(response.content().get(0).artistName()).isEqualTo("IU");
    }

    // -------------------------------------------------------------------------
    // getReleaseDetail
    // -------------------------------------------------------------------------

    @Test
    void should_return_release_detail_with_tracks_when_release_exists() {
        // given
        ReleaseGroup release = buildRelease(RELEASE_ID, ARTIST_ID, "ALBUM");
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Track track = buildTrack(1L, RELEASE_ID, 1);
        given(releaseGroupRepository.findById(RELEASE_ID)).willReturn(Optional.of(release));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(trackRepository.findByReleaseGroupIdOrderByPosition(RELEASE_ID)).willReturn(List.of(track));

        // when
        ReleaseDetailResponse response = releaseService.getReleaseDetail(RELEASE_ID);

        // then
        assertThat(response.id()).isEqualTo(RELEASE_ID);
        assertThat(response.title()).isEqualTo("미니앨범 " + RELEASE_ID);
        assertThat(response.type()).isEqualTo("ALBUM");
        assertThat(response.artistName()).isEqualTo("IU");
        assertThat(response.tracks()).hasSize(1);
        assertThat(response.tracks().get(0).title()).isEqualTo("트랙 1");
    }

    @Test
    void should_throw_release_not_found_when_release_does_not_exist() {
        // given
        given(releaseGroupRepository.findById(RELEASE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> releaseService.getReleaseDetail(RELEASE_ID))
                .isInstanceOf(ReleaseNotFoundException.class)
                .hasMessage(ErrorCode.RELEASE_NOT_FOUND.getMessage());
    }
}
