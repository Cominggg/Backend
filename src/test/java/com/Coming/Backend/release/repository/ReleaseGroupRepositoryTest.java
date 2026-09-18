package com.Coming.Backend.release.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.release.entity.ReleaseGroup;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReleaseGroupRepositoryTest {

    @Autowired
    private ReleaseGroupRepository releaseGroupRepository;

    private static final Long ARTIST_ID = 1L;

    private String likeQuery(String q) {
        return "%" + q.toLowerCase(Locale.ROOT) + "%";
    }

    private ReleaseGroup buildReleaseGroup(String title) {
        return ReleaseGroup.builder()
                .artistId(ARTIST_ID)
                .title(title)
                .build();
    }

    @Test
    void should_return_release_when_title_partially_matches_search_query_ignoring_case() {
        // given
        ReleaseGroup release = releaseGroupRepository.save(buildReleaseGroup("LILAC"));

        // when
        Page<ReleaseGroup> result = releaseGroupRepository.searchByTitleForMention(likeQuery("lil"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(ReleaseGroup::getId).contains(release.getId());
    }

    @Test
    void should_exclude_release_when_title_does_not_match_search_query() {
        // given
        ReleaseGroup matching = releaseGroupRepository.save(buildReleaseGroup("LILAC"));
        ReleaseGroup notMatching = releaseGroupRepository.save(buildReleaseGroup("Palette"));

        // when
        Page<ReleaseGroup> result = releaseGroupRepository.searchByTitleForMention(likeQuery("lil"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(ReleaseGroup::getId)
                .contains(matching.getId())
                .doesNotContain(notMatching.getId());
    }

    @Test
    void should_return_empty_page_when_no_release_matches_search_query() {
        // given
        releaseGroupRepository.save(buildReleaseGroup("LILAC"));

        // when
        Page<ReleaseGroup> result = releaseGroupRepository.searchByTitleForMention(likeQuery("존재하지않는검색어"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_match_wildcard_characters_literally_when_escaped_query_given() {
        // given
        ReleaseGroup release = releaseGroupRepository.save(buildReleaseGroup("100% Ready"));
        releaseGroupRepository.save(buildReleaseGroup("100 Ready"));
        String escapedQuery = "%" + "100\\% ready".toLowerCase(Locale.ROOT) + "%";

        // when
        Page<ReleaseGroup> result = releaseGroupRepository.searchByTitleForMention(escapedQuery, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(ReleaseGroup::getId).containsExactly(release.getId());
    }
}
