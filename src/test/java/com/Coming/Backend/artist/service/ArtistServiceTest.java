package com.Coming.Backend.artist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.artist.dto.FollowingArtistResponse;
import com.Coming.Backend.artist.exception.AlreadyFollowingException;
import com.Coming.Backend.artist.exception.NotFollowingException;

import com.Coming.Backend.artist.dto.ArtistConcertResponse;
import com.Coming.Backend.artist.dto.ArtistDetailResponse;
import com.Coming.Backend.artist.dto.ArtistSummaryResponse;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistUrl;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.ArtistUrlRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
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
class ArtistServiceTest {

    @InjectMocks
    private ArtistService artistService;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistUrlRepository artistUrlRepository;

    @Mock
    private UserFollowArtistRepository userFollowArtistRepository;

    @Mock
    private ConcertRepository concertRepository;

    private static final Long ARTIST_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 25);

    private Artist buildArtist(Long id, String name) {
        return Artist.builder()
                .id(id)
                .mbid("mbid-" + id)
                .name(name)
                .isComing(true)
                .build();
    }

    private Concert buildConcert(Long id, ConcertStatus status) {
        return Concert.builder()
                .id(id)
                .kopisId("kopis-" + id)
                .title("공연 " + id)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 3))
                .venueName("올림픽공원")
                .status(status)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // getArtists
    // -------------------------------------------------------------------------

    @Test
    void should_return_all_artists_when_name_is_null_and_user_is_not_authenticated() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findAll(PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, null, PAGEABLE, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("YOASOBI");
        assertThat(response.content().get(0).isFollowing()).isFalse();
        assertThat(response.content().get(0).imageUrl()).isNull();
    }

    @Test
    void should_return_filtered_artists_when_name_is_given() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase("yoa", PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists("yoa", null, null, PAGEABLE, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("YOASOBI");
    }

    @Test
    void should_return_artist_matched_by_alias_when_name_keyword_matches_alias() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "아이유");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findByNameOrAliasContainingIgnoreCase("IU", PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists("IU", null, null, PAGEABLE, null);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("아이유");
    }

    @Test
    void should_return_is_following_true_when_authenticated_user_follows_artist() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        UserFollowArtist follow = UserFollowArtist.builder()
                .userId(USER_ID)
                .artistId(ARTIST_ID)
                .build();
        given(artistRepository.findAll(PAGEABLE)).willReturn(page);
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, null, PAGEABLE, USER_ID);

        // then
        assertThat(response.content().get(0).isFollowing()).isTrue();
    }

    @Test
    void should_call_findByIsComing_true_when_isComing_is_true() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findByIsComing(true, PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, true, null, PAGEABLE, null);

        // then
        verify(artistRepository).findByIsComing(true, PAGEABLE);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("IU");
    }

    @Test
    void should_call_findByIsComing_false_when_isComing_is_false() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "Unknown Artist");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findByIsComing(false, PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, false, null, PAGEABLE, null);

        // then
        verify(artistRepository).findByIsComing(false, PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_findAllByIdIn_when_following_is_true_and_followingIds_exist() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(artistRepository.findAllByIdIn(List.of(ARTIST_ID), PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, true, PAGEABLE, USER_ID);

        // then
        verify(artistRepository).findAllByIdIn(List.of(ARTIST_ID), PAGEABLE);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).isFollowing()).isTrue();
    }

    @Test
    void should_return_empty_page_immediately_when_following_is_true_and_followingIds_empty() {
        // given
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, true, PAGEABLE, USER_ID);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(artistRepository, never()).findAllByIdIn(anyList(), any(Pageable.class));
    }

    @Test
    void should_return_empty_page_immediately_when_following_is_true_and_userId_is_null() {
        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, true, PAGEABLE, null);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(artistRepository, never()).findAllByIdIn(anyList(), any(Pageable.class));
    }

    @Test
    void should_call_findByIsComingAndNameOrAlias_when_name_and_isComing_given() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findByIsComingAndNameOrAliasContainingIgnoreCase(true, "IU", PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists("IU", true, null, PAGEABLE, null);

        // then
        verify(artistRepository).findByIsComingAndNameOrAliasContainingIgnoreCase(true, "IU", PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_findByIdInAndNameOrAlias_when_name_and_following_given() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(artistRepository.findByIdInAndNameOrAliasContainingIgnoreCase(List.of(ARTIST_ID), "IU", PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists("IU", null, true, PAGEABLE, USER_ID);

        // then
        verify(artistRepository).findByIdInAndNameOrAliasContainingIgnoreCase(List.of(ARTIST_ID), "IU", PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_findByIsComingAndIdIn_when_isComing_and_following_given() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(artistRepository.findByIsComingAndIdIn(true, List.of(ARTIST_ID), PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, true, true, PAGEABLE, USER_ID);

        // then
        verify(artistRepository).findByIsComingAndIdIn(true, List.of(ARTIST_ID), PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_call_findByIsComingAndIdInAndNameOrAlias_when_all_filters_given() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        UserFollowArtist follow = UserFollowArtist.builder().userId(USER_ID).artistId(ARTIST_ID).build();
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(artistRepository.findByIsComingAndIdInAndNameOrAliasContainingIgnoreCase(true, List.of(ARTIST_ID), "IU", PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(anyList(), anyString())).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists("IU", true, true, PAGEABLE, USER_ID);

        // then
        verify(artistRepository).findByIsComingAndIdInAndNameOrAliasContainingIgnoreCase(true, List.of(ARTIST_ID), "IU", PAGEABLE);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void should_return_spotify_url_when_artist_has_spotify_url() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        ArtistUrl spotifyUrl = ArtistUrl.builder()
                .artistId(ARTIST_ID)
                .type("spotify")
                .url("https://open.spotify.com/artist/xyz")
                .build();
        given(artistRepository.findAll(PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(List.of(ARTIST_ID), "spotify")).willReturn(List.of(spotifyUrl));

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, null, PAGEABLE, null);

        // then
        assertThat(response.content().get(0).spotifyUrl()).isEqualTo("https://open.spotify.com/artist/xyz");
    }

    @Test
    void should_return_null_spotify_url_when_artist_has_no_spotify_url() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        Page<Artist> page = new PageImpl<>(List.of(artist), PAGEABLE, 1);
        given(artistRepository.findAll(PAGEABLE)).willReturn(page);
        given(artistUrlRepository.findByArtistIdInAndType(List.of(ARTIST_ID), "spotify")).willReturn(List.of());

        // when
        PageResponse<ArtistSummaryResponse> response = artistService.getArtists(null, null, null, PAGEABLE, null);

        // then
        assertThat(response.content().get(0).spotifyUrl()).isNull();
    }

    // -------------------------------------------------------------------------
    // getArtist
    // -------------------------------------------------------------------------

    @Test
    void should_return_artist_detail_when_artist_exists() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(userFollowArtistRepository.countByArtistId(ARTIST_ID)).willReturn(100L);
        given(userFollowArtistRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(false);
        given(artistUrlRepository.findByArtistId(ARTIST_ID)).willReturn(List.of());

        // when
        ArtistDetailResponse response = artistService.getArtist(ARTIST_ID, USER_ID);

        // then
        assertThat(response.id()).isEqualTo(ARTIST_ID);
        assertThat(response.name()).isEqualTo("YOASOBI");
        assertThat(response.imageUrl()).isNull();
        assertThat(response.followersCount()).isEqualTo(100L);
        assertThat(response.isFollowing()).isFalse();
        assertThat(response.links()).isEmpty();
    }

    @Test
    void should_convert_url_type_to_display_label_correctly() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "YOASOBI");
        ArtistUrl spotifyUrl = ArtistUrl.builder()
                .artistId(ARTIST_ID)
                .type("spotify")
                .url("https://open.spotify.com/artist/xyz")
                .build();
        ArtistUrl youtubeUrl = ArtistUrl.builder()
                .artistId(ARTIST_ID)
                .type("youtube")
                .url("https://youtube.com/channel/xyz")
                .build();
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(artist));
        given(userFollowArtistRepository.countByArtistId(ARTIST_ID)).willReturn(0L);
        given(userFollowArtistRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(false);
        given(artistUrlRepository.findByArtistId(ARTIST_ID)).willReturn(List.of(spotifyUrl, youtubeUrl));

        // when
        ArtistDetailResponse response = artistService.getArtist(ARTIST_ID, USER_ID);

        // then
        assertThat(response.links()).hasSize(2);
        assertThat(response.links().get(0).label()).isEqualTo("Spotify");
        assertThat(response.links().get(1).label()).isEqualTo("YouTube");
    }

    @Test
    void should_throw_artist_not_found_exception_when_artist_does_not_exist_in_getArtist() {
        // given
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> artistService.getArtist(ARTIST_ID, USER_ID))
                .isInstanceOf(ArtistNotFoundException.class)
                .hasMessage(ErrorCode.ARTIST_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // getArtistConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_all_concerts_when_tab_is_all() {
        // given
        Concert concert = buildConcert(1L, ConcertStatus.ENDED);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertRepository.findAllByArtistId(eq(ARTIST_ID), any(LocalDate.class), anyList(), eq(PAGEABLE)))
                .willReturn(page);

        // when
        PageResponse<ArtistConcertResponse> response = artistService.getArtistConcerts(ARTIST_ID, "all", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("ENDED");
        assertThat(response.content().get(0).venue()).isEqualTo("올림픽공원");
    }

    @Test
    void should_return_upcoming_concerts_when_tab_is_upcoming() {
        // given
        Concert concert = buildConcert(2L, ConcertStatus.UPCOMING);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertRepository.findAllByArtistIdAndStatusIn(
                eq(ARTIST_ID),
                eq(List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING)),
                any(LocalDate.class),
                eq(PAGEABLE)
        )).willReturn(page);

        // when
        PageResponse<ArtistConcertResponse> response = artistService.getArtistConcerts(ARTIST_ID, "upcoming", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("UPCOMING");
    }

    @Test
    void should_return_past_concerts_when_tab_is_past() {
        // given
        Concert concert = buildConcert(3L, ConcertStatus.CANCELLED);
        Page<Concert> page = new PageImpl<>(List.of(concert), PAGEABLE, 1);
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(concertRepository.findAllByArtistIdAndStatusIn(
                eq(ARTIST_ID),
                eq(List.of(ConcertStatus.ENDED, ConcertStatus.CANCELLED)),
                any(LocalDate.class),
                eq(PAGEABLE)
        )).willReturn(page);

        // when
        PageResponse<ArtistConcertResponse> response = artistService.getArtistConcerts(ARTIST_ID, "past", PAGEABLE);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo("CANCELLED");
    }

    @Test
    void should_throw_artist_not_found_exception_when_artist_does_not_exist_in_getArtistConcerts() {
        // given
        given(artistRepository.existsById(ARTIST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> artistService.getArtistConcerts(ARTIST_ID, "all", PAGEABLE))
                .isInstanceOf(ArtistNotFoundException.class)
                .hasMessage(ErrorCode.ARTIST_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_invalid_input_exception_when_tab_is_unknown_value() {
        // given
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> artistService.getArtistConcerts(ARTIST_ID, "invalid", PAGEABLE))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());
    }

    // -------------------------------------------------------------------------
    // follow
    // -------------------------------------------------------------------------

    @Test
    void should_save_follow_when_artist_exists_and_not_following() {
        // given
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(userFollowArtistRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(false);

        // when
        artistService.follow(USER_ID, ARTIST_ID);

        // then
        verify(userFollowArtistRepository).save(any(UserFollowArtist.class));
    }

    @Test
    void should_throw_artist_not_found_when_artist_does_not_exist_in_follow() {
        // given
        given(artistRepository.existsById(ARTIST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> artistService.follow(USER_ID, ARTIST_ID))
                .isInstanceOf(ArtistNotFoundException.class)
                .hasMessage(ErrorCode.ARTIST_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_already_following_when_already_following() {
        // given
        given(artistRepository.existsById(ARTIST_ID)).willReturn(true);
        given(userFollowArtistRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> artistService.follow(USER_ID, ARTIST_ID))
                .isInstanceOf(AlreadyFollowingException.class)
                .hasMessage(ErrorCode.ALREADY_FOLLOWING.getMessage());
    }

    // -------------------------------------------------------------------------
    // unfollow
    // -------------------------------------------------------------------------

    @Test
    void should_delete_follow_when_following() {
        // given
        UserFollowArtist follow = UserFollowArtist.builder()
                .userId(USER_ID)
                .artistId(ARTIST_ID)
                .build();
        given(userFollowArtistRepository.findByUserIdAndArtistId(USER_ID, ARTIST_ID))
                .willReturn(Optional.of(follow));

        // when
        artistService.unfollow(USER_ID, ARTIST_ID);

        // then
        verify(userFollowArtistRepository).delete(follow);
    }

    @Test
    void should_throw_not_following_when_not_following() {
        // given
        given(userFollowArtistRepository.findByUserIdAndArtistId(USER_ID, ARTIST_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> artistService.unfollow(USER_ID, ARTIST_ID))
                .isInstanceOf(NotFollowingException.class)
                .hasMessage(ErrorCode.NOT_FOLLOWING.getMessage());
    }

    // -------------------------------------------------------------------------
    // getFollowingArtists
    // -------------------------------------------------------------------------

    @Test
    void should_return_following_artists() {
        // given
        Artist artist = buildArtist(ARTIST_ID, "IU");
        UserFollowArtist follow = UserFollowArtist.builder()
                .userId(USER_ID)
                .artistId(ARTIST_ID)
                .build();
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of(follow));
        given(artistRepository.findAllById(List.of(ARTIST_ID))).willReturn(List.of(artist));

        // when
        List<FollowingArtistResponse> response = artistService.getFollowingArtists(USER_ID);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(ARTIST_ID);
        assertThat(response.get(0).name()).isEqualTo("IU");
        assertThat(response.get(0).hasUpcomingConcert()).isTrue();
        assertThat(response.get(0).isFollowing()).isTrue();
    }

    @Test
    void should_return_empty_list_when_not_following_any() {
        // given
        given(userFollowArtistRepository.findByUserId(USER_ID)).willReturn(List.of());
        given(artistRepository.findAllById(List.of())).willReturn(List.of());

        // when
        List<FollowingArtistResponse> response = artistService.getFollowingArtists(USER_ID);

        // then
        assertThat(response).isEmpty();
    }
}
