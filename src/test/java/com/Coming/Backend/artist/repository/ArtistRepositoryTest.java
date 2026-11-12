package com.Coming.Backend.artist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.common.util.LikeEscapeUtils;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArtistRepositoryTest {

    @Autowired
    private ArtistRepository artistRepository;

    private Artist saveArtist(String name) {
        return artistRepository.save(Artist.builder()
                .mbid(UUID.randomUUID().toString())
                .name(name)
                .isComing(true)
                .build());
    }

    @Test
    void should_match_wildcards_literally_when_name_search_keyword_is_escaped() {
        // given
        Artist literal = saveArtist("ZQ142 100%_A");
        Artist wildcard = saveArtist("ZQ142 1000XA");

        // when
        Page<Artist> result = artistRepository.findByNameOrAliasContainingIgnoreCase(
                LikeEscapeUtils.escape("zq142 100%_a"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Artist::getId)
                .containsExactly(literal.getId())
                .doesNotContain(wildcard.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_match_wildcards_literally_when_filtered_name_search_keyword_is_escaped() {
        // given
        Artist literal = saveArtist("ZQ142 50%_B");
        Artist wildcard = saveArtist("ZQ142 500XB");
        List<Long> ids = List.of(literal.getId(), wildcard.getId());

        // when
        Page<Artist> result = artistRepository.findByIsComingAndIdInAndNameOrAliasContainingIgnoreCase(
                true, ids, LikeEscapeUtils.escape("50%_b"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Artist::getId).containsExactly(literal.getId());
    }

    @Test
    void should_match_wildcards_literally_when_follower_count_sorted_search_keyword_is_escaped() {
        // given
        Artist literal = saveArtist("ZQ142 70%_C");
        saveArtist("ZQ142 700XC");

        // when
        Page<Artist> result = artistRepository.findAllOrderByFollowerCount(
                true, LikeEscapeUtils.escape("zq142 70%_c"), null, null, true, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Artist::getId).containsExactly(literal.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
