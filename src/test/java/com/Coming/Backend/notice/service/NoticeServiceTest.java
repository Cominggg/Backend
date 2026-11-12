package com.Coming.Backend.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.notice.dto.NoticeDetailResponse;
import com.Coming.Backend.notice.dto.NoticeSummaryResponse;
import com.Coming.Backend.notice.entity.Notice;
import com.Coming.Backend.notice.exception.NoticeNotFoundException;
import com.Coming.Backend.notice.repository.NoticeRepository;
import com.Coming.Backend.post.service.EntityLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private EntityLookupService entityLookupService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long AUTHOR_ID = 1L;
    private static final Long NOTICE_ID = 10L;

    private Notice buildNotice(Long id, String title, String content, boolean active) {
        Notice notice = Notice.builder()
                .userId(AUTHOR_ID)
                .title(title)
                .content(content)
                .active(active)
                .build();
        ReflectionTestUtils.setField(notice, "id", id);
        return notice;
    }

    private static String card(String entityType, long entityId) {
        return "{\"type\":\"entityMentionCard\",\"attrs\":{\"entityType\":\"" + entityType
                + "\",\"entityId\":" + entityId + ",\"title\":\"카드 제목\"}}";
    }

    private static String paragraph(String text) {
        return "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}]}";
    }

    private static String doc(String... blocks) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    // -------------------------------------------------------------------------
    // getRecent
    // -------------------------------------------------------------------------

    @Test
    void should_return_summary_responses_when_active_notices_found() {
        // given
        Notice notice = buildNotice(NOTICE_ID, "점검 안내", "내용", true);
        given(noticeRepository.findActiveNotices(PageRequest.of(0, 5))).willReturn(List.of(notice));

        // when
        List<NoticeSummaryResponse> response = noticeService.getRecent(5);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(NOTICE_ID);
        assertThat(response.get(0).title()).isEqualTo("점검 안내");
    }

    @Test
    void should_return_empty_list_when_no_active_notices_found() {
        // given
        given(noticeRepository.findActiveNotices(PageRequest.of(0, 5))).willReturn(List.of());

        // when
        List<NoticeSummaryResponse> response = noticeService.getRecent(5);

        // then
        assertThat(response).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getDetail
    // -------------------------------------------------------------------------

    @Test
    void should_throw_notice_not_found_exception_when_notice_does_not_exist() {
        // given
        given(noticeRepository.findById(NOTICE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getDetail(NOTICE_ID))
                .isInstanceOf(NoticeNotFoundException.class)
                .hasMessage(ErrorCode.NOTICE_NOT_FOUND.getMessage());
    }

    @Test
    void should_return_notice_detail_when_active_notice_found() throws Exception {
        // given
        String content = doc(paragraph("점검은 새벽 2시부터 진행됩니다"));
        Notice notice = buildNotice(NOTICE_ID, "점검 안내", content, true);
        given(noticeRepository.findById(NOTICE_ID)).willReturn(Optional.of(notice));
        given(entityLookupService.findVisibleConcertIds(Set.of())).willReturn(Set.of());

        // when
        NoticeDetailResponse response = noticeService.getDetail(NOTICE_ID);

        // then
        assertThat(response.id()).isEqualTo(NOTICE_ID);
        assertThat(response.title()).isEqualTo("점검 안내");
        assertThat(response.content()).isEqualTo(objectMapper.readValue(content, Object.class));
    }

    @Test
    void should_remove_hidden_and_missing_concert_cards_when_notice_content_has_concert_mentions() throws Exception {
        // given
        Notice notice = buildNotice(NOTICE_ID, "공연 소개", doc(
                paragraph("이번 주 공연"),
                card("CONCERT", 7L),
                card("CONCERT", 8L),
                card("CONCERT", 9L),
                card("ARTIST", 3L)
        ), true);
        given(noticeRepository.findById(NOTICE_ID)).willReturn(Optional.of(notice));
        // 8은 숨김 공연, 9는 없는 공연이라 공개 공연으로 7만 돌아온다
        given(entityLookupService.findVisibleConcertIds(Set.of(7L, 8L, 9L))).willReturn(Set.of(7L));

        // when
        NoticeDetailResponse response = noticeService.getDetail(NOTICE_ID);

        // then
        Object expected = objectMapper.readValue(
                doc(paragraph("이번 주 공연"), card("CONCERT", 7L), card("ARTIST", 3L)), Object.class);
        assertThat(response.content()).isEqualTo(expected);
    }

    @Test
    void should_throw_notice_not_found_exception_when_notice_exists_but_inactive() {
        // given
        Notice notice = buildNotice(NOTICE_ID, "종료된 공지", "내용", false);
        given(noticeRepository.findById(NOTICE_ID)).willReturn(Optional.of(notice));

        // when & then
        assertThatThrownBy(() -> noticeService.getDetail(NOTICE_ID))
                .isInstanceOf(NoticeNotFoundException.class)
                .hasMessage(ErrorCode.NOTICE_NOT_FOUND.getMessage());
    }
}
