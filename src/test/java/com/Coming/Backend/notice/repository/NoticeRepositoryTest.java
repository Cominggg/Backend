package com.Coming.Backend.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.notice.entity.Notice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    private static final Long AUTHOR_ID = 1L;

    private Notice buildNotice(String title, boolean active) {
        return Notice.builder()
                .userId(AUTHOR_ID)
                .title(title)
                .content("공지 내용")
                .active(active)
                .build();
    }

    @Test
    void should_return_active_notice_when_finding_active_notices() {
        // given
        Notice activeNotice = noticeRepository.save(buildNotice("점검 안내", true));

        // when
        var result = noticeRepository.findActiveNotices(PageRequest.of(0, 20));

        // then
        assertThat(result).extracting(Notice::getId).contains(activeNotice.getId());
    }

    @Test
    void should_exclude_inactive_notice_when_finding_active_notices() {
        // given
        Notice inactiveNotice = noticeRepository.save(buildNotice("종료된 공지", false));

        // when
        var result = noticeRepository.findActiveNotices(PageRequest.of(0, 20));

        // then
        assertThat(result).extracting(Notice::getId).doesNotContain(inactiveNotice.getId());
    }

    @Test
    void should_return_active_notices_ordered_by_created_at_desc() throws InterruptedException {
        // given
        Notice olderNotice = noticeRepository.save(buildNotice("먼저 등록된 공지", true));
        Thread.sleep(10);
        Notice newerNotice = noticeRepository.save(buildNotice("나중에 등록된 공지", true));

        // when
        var result = noticeRepository.findActiveNotices(PageRequest.of(0, 20));

        // then
        assertThat(result).extracting(Notice::getId)
                .containsSubsequence(newerNotice.getId(), olderNotice.getId());
    }
}
