package com.Coming.Backend.release.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.release.entity.Track;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TrackRepositoryTest {

    @Autowired
    private TrackRepository trackRepository;

    private static final Long RELEASE_GROUP_ID = 1L;

    private String likeQuery(String q) {
        return "%" + q.toLowerCase(Locale.ROOT) + "%";
    }

    private Track buildTrack(String title) {
        return Track.builder()
                .releaseGroupId(RELEASE_GROUP_ID)
                .title(title)
                .position(1)
                .build();
    }

    @Test
    void should_return_track_when_title_partially_matches_search_query_ignoring_case() {
        // given
        Track track = trackRepository.save(buildTrack("Dynamite"));

        // when
        Page<Track> result = trackRepository.searchByTitleForMention(likeQuery("DYNA"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Track::getId).contains(track.getId());
    }

    @Test
    void should_exclude_track_when_title_does_not_match_search_query() {
        // given
        Track matching = trackRepository.save(buildTrack("Dynamite"));
        Track notMatching = trackRepository.save(buildTrack("Butter"));

        // when
        Page<Track> result = trackRepository.searchByTitleForMention(likeQuery("Dyna"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Track::getId)
                .contains(matching.getId())
                .doesNotContain(notMatching.getId());
    }

    @Test
    void should_return_empty_page_when_no_track_matches_search_query() {
        // given
        trackRepository.save(buildTrack("Dynamite"));

        // when
        Page<Track> result = trackRepository.searchByTitleForMention(likeQuery("존재하지않는검색어"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_match_wildcard_characters_literally_when_escaped_query_given() {
        // given
        Track track = trackRepository.save(buildTrack("100% Ready"));
        trackRepository.save(buildTrack("100 Ready"));
        String escapedQuery = "%" + "100\\% ready".toLowerCase(Locale.ROOT) + "%";

        // when
        Page<Track> result = trackRepository.searchByTitleForMention(escapedQuery, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Track::getId).containsExactly(track.getId());
    }
}
