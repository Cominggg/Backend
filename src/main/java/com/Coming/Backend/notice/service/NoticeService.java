package com.Coming.Backend.notice.service;

import com.Coming.Backend.common.util.TiptapConcertMentionFilter;
import com.Coming.Backend.common.util.TiptapContentCodec;
import com.Coming.Backend.notice.dto.NoticeDetailResponse;
import com.Coming.Backend.notice.dto.NoticeSummaryResponse;
import com.Coming.Backend.notice.entity.Notice;
import com.Coming.Backend.notice.exception.NoticeNotFoundException;
import com.Coming.Backend.notice.repository.NoticeRepository;
import com.Coming.Backend.post.service.EntityLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final EntityLookupService entityLookupService;

    /**
     * 활성화된 공지사항을 최신순으로 상위 N개 조회한다. 커뮤니티 홈 상단 고정 노출용.
     */
    public List<NoticeSummaryResponse> getRecent(int limit) {
        List<Notice> notices = noticeRepository.findActiveNotices(PageRequest.of(0, limit));
        return notices.stream()
                .map(notice -> new NoticeSummaryResponse(notice.getId(), notice.getTitle(), notice.getCreatedAt()))
                .toList();
    }

    /**
     * 공지사항 상세를 조회한다. 비활성 공지는 일반 사용자에게 노출하지 않는다.
     * 본문의 공연 멘션 중 숨김 상태이거나 없는 공연은 응답에서 뺀다(DB 본문은 그대로 둔다).
     */
    public NoticeDetailResponse getDetail(Long id) {
        Notice notice = noticeRepository.findById(id).orElseThrow(NoticeNotFoundException::new);
        if (!notice.isActive()) {
            throw new NoticeNotFoundException();
        }
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                removeUnavailableConcertMentions(TiptapContentCodec.read(notice.getContent())),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private Object removeUnavailableConcertMentions(Object content) {
        Set<Long> mentionedConcertIds = TiptapConcertMentionFilter.collectConcertIds(content);
        Set<Long> visibleConcertIds = entityLookupService.findVisibleConcertIds(mentionedConcertIds);
        // 유효한 id가 하나도 없어도 필터를 돌려야 id가 null·누락·문자열인 공연 멘션이 빠진다
        return TiptapConcertMentionFilter.removeConcertMentionsExcept(content, visibleConcertIds);
    }
}
