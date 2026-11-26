package com.Coming.Backend.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.notice.entity.Notice;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long AUTHOR_ID = 1L;
    private static final String CONTENT_JSON =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"공지 내용\"}]}]}";

    private Notice buildNotice(String title, boolean active) {
        return Notice.builder()
                .userId(AUTHOR_ID)
                .title(title)
                .content(CONTENT_JSON)
                .active(active)
                .build();
    }

    @Test
    void should_read_saved_tiptap_json_content_when_finding_notice_by_id() throws Exception {
        // given
        Notice saved = noticeRepository.save(buildNotice("점검 안내", true));
        entityManager.flush();
        entityManager.clear();

        // when
        Notice found = noticeRepository.findById(saved.getId()).orElseThrow();

        // then
        // jsonb는 키 순서·공백을 정규화할 수 있어 트리로 비교한다
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(objectMapper.readTree(found.getContent())).isEqualTo(objectMapper.readTree(CONTENT_JSON));
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
