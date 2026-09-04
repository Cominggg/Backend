package com.Coming.Backend.artist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.artist.dto.ArtistConcertResponse;
import com.Coming.Backend.artist.dto.ArtistDetailResponse;
import com.Coming.Backend.artist.dto.ArtistLinkDto;
import com.Coming.Backend.artist.dto.ArtistSummaryResponse;
import com.Coming.Backend.artist.dto.FollowingArtistResponse;
import com.Coming.Backend.artist.exception.AlreadyFollowingException;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.exception.NotFollowingException;
import com.Coming.Backend.artist.service.ArtistService;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.release.dto.ArtistReleaseItemResponse;
import com.Coming.Backend.release.dto.TrackDto;
import com.Coming.Backend.release.service.ReleaseService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ArtistControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ArtistService artistService;

    @Mock
    private ReleaseService releaseService;

    @InjectMocks
    private ArtistController artistController;

    private static final Long ARTIST_ID = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(artistController)
                .setControllerAdvice(new GlobalExceptionHandler(new com.Coming.Backend.common.discord.NoOpDiscordNotifier()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // -------------------------------------------------------------------------
    // getArtists
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_artist_list_when_no_name_filter() throws Exception {
        // given
        ArtistSummaryResponse summary = new ArtistSummaryResponse(ARTIST_ID, "YOASOBI", null, null, true, false, null, 0L);
        PageResponse<ArtistSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 25, 1, 1);
        given(artistService.getArtists(isNull(), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/artists").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ARTIST_ID))
                .andExpect(jsonPath("$.content[0].name").value("YOASOBI"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_filtered_artist_list_when_name_given() throws Exception {
        // given
        ArtistSummaryResponse summary = new ArtistSummaryResponse(ARTIST_ID, "YOASOBI", null, null, true, false, null, 0L);
        PageResponse<ArtistSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 25, 1, 1);
        given(artistService.getArtists(eq("yoa"), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/artists").param("name", "yoa").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("YOASOBI"));
    }

    @Test
    void should_return_400_when_sort_property_is_not_allowed() throws Exception {
        // when & then
        mockMvc.perform(get("/api/artists").param("sort", "notAllowedField").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_delegate_to_service_when_sort_is_followerCount_desc() throws Exception {
        // given
        ArtistSummaryResponse summary = new ArtistSummaryResponse(ARTIST_ID, "YOASOBI", null, null, true, false, null, 10L);
        PageResponse<ArtistSummaryResponse> pageResponse = new PageResponse<>(List.of(summary), 0, 25, 1, 1);
        given(artistService.getArtists(isNull(), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/artists").param("sort", "followerCount,desc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].followerCount").value(10));
    }

    // -------------------------------------------------------------------------
    // getArtist
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_artist_detail_when_artist_exists() throws Exception {
        // given
        ArtistDetailResponse detail = new ArtistDetailResponse(
                ARTIST_ID, "YOASOBI", null, null, true, false, 500L,
                List.of(new ArtistLinkDto("spotify", "Spotify", "https://spotify.com"))
        );
        given(artistService.getArtist(eq(ARTIST_ID), isNull())).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/artists/{id}", ARTIST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ARTIST_ID))
                .andExpect(jsonPath("$.name").value("YOASOBI"))
                .andExpect(jsonPath("$.followersCount").value(500));
    }

    @Test
    void should_return_404_when_artist_not_found() throws Exception {
        // given
        given(artistService.getArtist(eq(999L), isNull())).willThrow(new ArtistNotFoundException());

        // when & then
        mockMvc.perform(get("/api/artists/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ARTIST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // getArtistConcerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_concert_list_when_tab_is_all() throws Exception {
        // given
        ArtistConcertResponse concert = new ArtistConcertResponse(
                1L, "YOASOBI CONCERT",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 3),
                "올림픽공원", "공연완료"
        );
        PageResponse<ArtistConcertResponse> pageResponse = new PageResponse<>(List.of(concert), 0, 10, 1, 1);
        given(artistService.getArtistConcerts(eq(ARTIST_ID), eq("all"), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/artists/{id}/concerts", ARTIST_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("YOASOBI CONCERT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_404_when_artist_not_found_in_concerts() throws Exception {
        // given
        given(artistService.getArtistConcerts(eq(999L), eq("all"), any(Pageable.class)))
                .willThrow(new ArtistNotFoundException());

        // when & then
        mockMvc.perform(get("/api/artists/{id}/concerts", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ARTIST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_tab_is_invalid() throws Exception {
        // given
        given(artistService.getArtistConcerts(eq(ARTIST_ID), eq("invalid"), any(Pageable.class)))
                .willThrow(new InvalidInputException());

        // when & then
        mockMvc.perform(get("/api/artists/{id}/concerts", ARTIST_ID)
                        .param("tab", "invalid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // follow
    // -------------------------------------------------------------------------

    @Test
    void should_return_204_when_follow_succeeds() throws Exception {
        // given
        doNothing().when(artistService).follow(isNull(), eq(ARTIST_ID));

        // when & then
        mockMvc.perform(post("/api/artists/{id}/follow", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_404_when_artist_not_found_in_follow() throws Exception {
        // given
        doThrow(new ArtistNotFoundException()).when(artistService).follow(isNull(), eq(999L));

        // when & then
        mockMvc.perform(post("/api/artists/{id}/follow", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ARTIST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_409_when_already_following() throws Exception {
        // given
        doThrow(new AlreadyFollowingException()).when(artistService).follow(isNull(), eq(ARTIST_ID));

        // when & then
        mockMvc.perform(post("/api/artists/{id}/follow", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ALREADY_FOLLOWING.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // unfollow
    // -------------------------------------------------------------------------

    @Test
    void should_return_204_when_unfollow_succeeds() throws Exception {
        // given
        doNothing().when(artistService).unfollow(isNull(), eq(ARTIST_ID));

        // when & then
        mockMvc.perform(delete("/api/artists/{id}/follow", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_not_following() throws Exception {
        // given
        doThrow(new NotFollowingException()).when(artistService).unfollow(isNull(), eq(ARTIST_ID));

        // when & then
        mockMvc.perform(delete("/api/artists/{id}/follow", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOLLOWING.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // getArtistReleases
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_artist_releases_when_no_type_filter() throws Exception {
        // given
        TrackDto track = new TrackDto(1, "Idol", 210000, 1, false, null);
        ArtistReleaseItemResponse item = new ArtistReleaseItemResponse(
                1L, "LILAC", "ALBUM", LocalDate.of(2021, 3, 25),
                "https://cover.example.com/1", null, List.of(track)
        );
        PageResponse<ArtistReleaseItemResponse> pageResponse =
                new PageResponse<>(List.of(item), 0, 10, 1, 1);
        given(releaseService.getArtistReleases(eq(ARTIST_ID), eq(List.of()), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/artists/{id}/releases", ARTIST_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("LILAC"))
                .andExpect(jsonPath("$.content[0].tracks").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_404_when_artist_not_found_in_getArtistReleases() throws Exception {
        // given
        given(releaseService.getArtistReleases(eq(999L), eq(List.of()), any(Pageable.class)))
                .willThrow(new ArtistNotFoundException());

        // when & then
        mockMvc.perform(get("/api/artists/{id}/releases", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ARTIST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // getFollowingArtists
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_following_artist_list() throws Exception {
        // given
        List<FollowingArtistResponse> following = List.of(
                new FollowingArtistResponse(ARTIST_ID, "IU", null, null, true, true)
        );
        given(artistService.getFollowingArtists(isNull())).willReturn(following);

        // when & then
        mockMvc.perform(get("/api/artists/following")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ARTIST_ID))
                .andExpect(jsonPath("$[0].name").value("IU"))
                .andExpect(jsonPath("$[0].hasUpcomingConcert").value(true));
    }
}
