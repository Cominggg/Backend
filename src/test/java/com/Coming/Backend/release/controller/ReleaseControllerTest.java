package com.Coming.Backend.release.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.release.dto.ReleaseDetailResponse;
import com.Coming.Backend.release.dto.ReleaseListItemResponse;
import com.Coming.Backend.release.dto.TrackDto;
import com.Coming.Backend.release.exception.ReleaseNotFoundException;
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
class ReleaseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReleaseService releaseService;

    @InjectMocks
    private ReleaseController releaseController;

    private static final Long ARTIST_ID = 1L;
    private static final Long RELEASE_ID = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(releaseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/releases/search
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_search_results_when_query_matches() throws Exception {
        // given
        ReleaseListItemResponse item = new ReleaseListItemResponse(
                RELEASE_ID, "https://cover.example.com/10", "IU",
                "LILAC", "Album", LocalDate.of(2021, 3, 25)
        );
        PageResponse<ReleaseListItemResponse> pageResponse =
                new PageResponse<>(List.of(item), 0, 20, 1, 1);
        given(releaseService.searchReleases(eq("IU"), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/releases/search")
                        .param("q", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(RELEASE_ID))
                .andExpect(jsonPath("$.content[0].artistName").value("IU"))
                .andExpect(jsonPath("$.content[0].title").value("LILAC"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_empty_results_when_query_matches_nothing() throws Exception {
        // given
        PageResponse<ReleaseListItemResponse> emptyPage =
                new PageResponse<>(List.of(), 0, 20, 0, 0);
        given(releaseService.searchReleases(eq("없는앨범"), any(Pageable.class)))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/releases/search")
                        .param("q", "없는앨범")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void should_return_400_when_q_is_missing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/releases/search")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
    }

    // -------------------------------------------------------------------------
    // GET /api/releases
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_all_releases_when_no_filters() throws Exception {
        // given
        ReleaseListItemResponse item = new ReleaseListItemResponse(
                RELEASE_ID, "https://cover.example.com/10", "IU",
                "LILAC", "ALBUM", LocalDate.of(2021, 3, 25)
        );
        PageResponse<ReleaseListItemResponse> pageResponse =
                new PageResponse<>(List.of(item), 0, 20, 1, 1);
        given(releaseService.getReleases(isNull(), isNull(), isNull(), eq(false), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/releases")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(RELEASE_ID))
                .andExpect(jsonPath("$.content[0].artistName").value("IU"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_releases_filtered_by_type() throws Exception {
        // given
        ReleaseListItemResponse item = new ReleaseListItemResponse(
                RELEASE_ID, "https://cover.example.com/10", "IU",
                "LILAC", "ALBUM", LocalDate.of(2021, 3, 25)
        );
        PageResponse<ReleaseListItemResponse> pageResponse =
                new PageResponse<>(List.of(item), 0, 20, 1, 1);
        given(releaseService.getReleases(isNull(), eq("ALBUM"), isNull(), eq(false), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/releases")
                        .param("type", "ALBUM")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("ALBUM"));
    }

    @Test
    void should_return_200_with_releases_filtered_by_type_other() throws Exception {
        // given
        ReleaseListItemResponse item = new ReleaseListItemResponse(
                RELEASE_ID, "https://cover.example.com/10", "IU",
                "Live at Seoul", "LIVE", LocalDate.of(2023, 8, 1)
        );
        PageResponse<ReleaseListItemResponse> pageResponse =
                new PageResponse<>(List.of(item), 0, 20, 1, 1);
        given(releaseService.getReleases(isNull(), eq("기타"), isNull(), eq(false), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/releases")
                        .param("type", "기타")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("LIVE"));
    }

    @Test
    void should_return_200_with_following_releases_when_following_is_true() throws Exception {
        // given
        ReleaseListItemResponse item = new ReleaseListItemResponse(
                RELEASE_ID, "https://cover.example.com/10", "IU",
                "LILAC", "ALBUM", LocalDate.of(2021, 3, 25)
        );
        PageResponse<ReleaseListItemResponse> pageResponse =
                new PageResponse<>(List.of(item), 0, 20, 1, 1);
        given(releaseService.getReleases(isNull(), isNull(), isNull(), eq(true), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/releases")
                        .param("following", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].artistName").value("IU"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // GET /api/releases/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_release_detail_when_release_exists() throws Exception {
        // given
        TrackDto track1 = new TrackDto(1, "Coin", 196000, 1, false);
        TrackDto track2 = new TrackDto(2, "Celebrity", 197000, 1, false);
        ReleaseDetailResponse detail = new ReleaseDetailResponse(
                RELEASE_ID, "LILAC", "Album", LocalDate.of(2021, 3, 25),
                "https://cover.example.com/10", "KAKAO M", 2,
                ARTIST_ID, "IU", List.of(track1, track2)
        );
        given(releaseService.getReleaseDetail(eq(RELEASE_ID))).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/releases/{id}", RELEASE_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RELEASE_ID))
                .andExpect(jsonPath("$.title").value("LILAC"))
                .andExpect(jsonPath("$.type").value("Album"))
                .andExpect(jsonPath("$.label").value("KAKAO M"))
                .andExpect(jsonPath("$.artistId").value(ARTIST_ID))
                .andExpect(jsonPath("$.artistName").value("IU"))
                .andExpect(jsonPath("$.tracks").isArray())
                .andExpect(jsonPath("$.tracks.length()").value(2))
                .andExpect(jsonPath("$.tracks[0].position").value(1))
                .andExpect(jsonPath("$.tracks[0].title").value("Coin"))
                .andExpect(jsonPath("$.tracks[1].position").value(2));
    }

    @Test
    void should_return_404_when_release_not_found() throws Exception {
        // given
        given(releaseService.getReleaseDetail(eq(999L))).willThrow(new ReleaseNotFoundException());

        // when & then
        mockMvc.perform(get("/api/releases/{id}", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RELEASE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
