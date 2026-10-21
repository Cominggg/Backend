package com.Coming.Backend.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConcertRepositoryTest {

    @Autowired
    private ConcertRepository concertRepository;

    private static final List<ConcertStatus> HIDDEN = List.of(ConcertStatus.EXCLUDED, ConcertStatus.PENDING);

    // 로컬 DB의 기존 공연보다 앞에 오도록 먼 미래 날짜로 저장하고 startDate DESC로 조회한다
    private static final LocalDate FAR_FUTURE = LocalDate.of(2999, 12, 31);
    private static final Pageable FAR_FUTURE_FIRST = PageRequest.of(0, 50,
            Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by("id")));

    private final String uniqueTitle = "issue137-" + UUID.randomUUID();

    private Concert saveConcert(ConcertStatus status) {
        return saveConcert(status, FAR_FUTURE);
    }

    private Concert saveConcert(ConcertStatus status, LocalDate startDate) {
        return concertRepository.save(Concert.builder()
                .title(uniqueTitle + " " + status.name())
                .startDate(startDate)
                .endDate(startDate)
                .venueName("올림픽공원 KSPO DOME")
                .status(status)
                .viewCount(0L)
                .kopisUpdateDate(LocalDate.of(2026, 10, 1))
                .build());
    }

    private Page<Concert> findConcerts(List<ConcertStatus> statuses) {
        return concertRepository.findConcerts(HIDDEN, statuses, null, null, false, LocalDateTime.now(), FAR_FUTURE_FIRST);
    }

    private String likeQuery() {
        return "%" + uniqueTitle.toLowerCase(Locale.ROOT) + "%";
    }

    @Test
    void should_return_concerts_of_all_given_statuses_when_multiple_statuses_given() {
        // given
        Concert upcoming = saveConcert(ConcertStatus.UPCOMING);
        Concert ongoing = saveConcert(ConcertStatus.ONGOING);
        Concert ended = saveConcert(ConcertStatus.ENDED);

        // when
        Page<Concert> result = findConcerts(List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING));

        // then
        assertThat(result.getContent()).extracting(Concert::getId)
                .contains(upcoming.getId(), ongoing.getId())
                .doesNotContain(ended.getId());
    }

    @Test
    void should_return_only_matching_status_when_single_status_given() {
        // given
        Concert upcoming = saveConcert(ConcertStatus.UPCOMING);
        Concert ongoing = saveConcert(ConcertStatus.ONGOING);
        Concert ended = saveConcert(ConcertStatus.ENDED);

        // when
        Page<Concert> result = findConcerts(List.of(ConcertStatus.UPCOMING));

        // then
        assertThat(result.getContent()).extracting(Concert::getId)
                .contains(upcoming.getId())
                .doesNotContain(ongoing.getId(), ended.getId());
    }

    @Test
    void should_return_all_visible_statuses_when_statuses_is_null() {
        // given
        Concert upcoming = saveConcert(ConcertStatus.UPCOMING);
        Concert ongoing = saveConcert(ConcertStatus.ONGOING);
        Concert ended = saveConcert(ConcertStatus.ENDED);
        Concert cancelled = saveConcert(ConcertStatus.CANCELLED);
        Concert excluded = saveConcert(ConcertStatus.EXCLUDED);
        Concert pending = saveConcert(ConcertStatus.PENDING);

        // when
        Page<Concert> result = findConcerts(null);

        // then
        assertThat(result.getContent()).extracting(Concert::getId)
                .contains(upcoming.getId(), ongoing.getId(), ended.getId(), cancelled.getId())
                .doesNotContain(excluded.getId(), pending.getId());
    }

    @Test
    void should_exclude_hidden_concerts_when_hidden_statuses_requested() {
        // given
        Concert upcoming = saveConcert(ConcertStatus.UPCOMING);
        Concert excluded = saveConcert(ConcertStatus.EXCLUDED);
        Concert pending = saveConcert(ConcertStatus.PENDING);

        // when
        Page<Concert> result = findConcerts(List.of(ConcertStatus.UPCOMING, ConcertStatus.EXCLUDED, ConcertStatus.PENDING));

        // then
        assertThat(result.getContent()).extracting(Concert::getId)
                .contains(upcoming.getId())
                .doesNotContain(excluded.getId(), pending.getId());
    }

    @Test
    void should_return_matching_concerts_and_total_count_when_search_with_multiple_statuses() {
        // given
        Concert upcoming = saveConcert(ConcertStatus.UPCOMING);
        saveConcert(ConcertStatus.ONGOING);
        saveConcert(ConcertStatus.ENDED);
        saveConcert(ConcertStatus.EXCLUDED);

        // when
        Page<Concert> result = concertRepository.searchConcerts(HIDDEN, List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING),
                null, null, false, LocalDateTime.now(), likeQuery(), PageRequest.of(0, 1, Sort.by("id")));

        // then
        assertThat(result.getContent()).extracting(Concert::getId).containsExactly(upcoming.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void should_return_all_concerts_in_id_order_without_duplicates_when_paging_concerts_with_same_start_date() {
        // given
        LocalDate sameDate = LocalDate.of(2027, 5, 1);
        List<Long> savedIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            savedIds.add(saveConcert(ConcertStatus.UPCOMING, sameDate).getId());
        }
        Sort sort = Sort.by("startDate").and(Sort.by("id"));

        // when
        List<Long> pagedIds = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            Page<Concert> result = concertRepository.searchConcerts(HIDDEN, null, null, null, false, LocalDateTime.now(),
                    likeQuery(), PageRequest.of(page, 2, sort));
            result.getContent().forEach(concert -> pagedIds.add(concert.getId()));
        }

        // then
        assertThat(pagedIds).containsExactlyElementsOf(savedIds.stream().sorted().toList());
    }
}
