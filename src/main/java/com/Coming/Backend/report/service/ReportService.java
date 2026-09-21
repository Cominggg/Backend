package com.Coming.Backend.report.service;

import com.Coming.Backend.post.repository.CommentRepository;
import com.Coming.Backend.post.repository.PostRepository;
import com.Coming.Backend.report.dto.ReportCreateRequest;
import com.Coming.Backend.report.dto.ReportCreateResponse;
import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;
import com.Coming.Backend.report.event.ReportCreatedEvent;
import com.Coming.Backend.report.exception.ReportAlreadyExistsException;
import com.Coming.Backend.report.exception.ReportDetailRequiredException;
import com.Coming.Backend.report.exception.ReportTargetNotFoundException;
import com.Coming.Backend.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 게시글·댓글을 신고한다. reason이 ETC인데 detail이 비어 있으면 ReportDetailRequiredException,
     * 대상이 존재하지 않으면 ReportTargetNotFoundException, 이미 신고한 대상이면
     * ReportAlreadyExistsException을 던진다.
     */
    @Transactional
    public ReportCreateResponse create(Long reporterId, ReportCreateRequest request) {
        if (request.reason() == ReportReason.ETC && (request.detail() == null || request.detail().isBlank())) {
            throw new ReportDetailRequiredException();
        }
        if (!targetExists(request.targetType(), request.targetId())) {
            throw new ReportTargetNotFoundException();
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, request.targetType(), request.targetId())) {
            throw new ReportAlreadyExistsException();
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(request.detail())
                .status(ReportStatus.PENDING)
                .build();
        try {
            reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            throw new ReportAlreadyExistsException();
        }

        eventPublisher.publishEvent(new ReportCreatedEvent(report));
        return new ReportCreateResponse(report.getId());
    }

    private boolean targetExists(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
        };
    }
}
