package com.Coming.Backend.notice.service;

import com.Coming.Backend.notice.dto.NoticeDetailResponse;
import com.Coming.Backend.notice.dto.NoticeSummaryResponse;
import com.Coming.Backend.notice.entity.Notice;
import com.Coming.Backend.notice.exception.NoticeNotFoundException;
import com.Coming.Backend.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

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
     */
    public NoticeDetailResponse getDetail(Long id) {
        Notice notice = noticeRepository.findById(id).orElseThrow(NoticeNotFoundException::new);
        if (!notice.isActive()) {
            throw new NoticeNotFoundException();
        }
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
