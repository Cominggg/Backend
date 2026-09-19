package com.Coming.Backend.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.post.repository.CommentRepository;
import com.Coming.Backend.post.repository.PostRepository;
import com.Coming.Backend.report.dto.ReportCreateRequest;
import com.Coming.Backend.report.dto.ReportCreateResponse;
import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportTargetType;
import com.Coming.Backend.report.event.ReportCreatedEvent;
import com.Coming.Backend.report.exception.ReportAlreadyExistsException;
import com.Coming.Backend.report.exception.ReportDetailRequiredException;
import com.Coming.Backend.report.exception.ReportTargetNotFoundException;
import com.Coming.Backend.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long REPORTER_ID = 1L;
    private static final Long REPORT_ID = 100L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 20L;

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void should_throw_report_detail_required_exception_when_reason_is_etc_and_detail_is_null() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.ETC, null);

        // when & then
        assertThatThrownBy(() -> reportService.create(REPORTER_ID, request))
                .isInstanceOf(ReportDetailRequiredException.class)
                .hasMessage(ErrorCode.REPORT_DETAIL_REQUIRED.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void should_throw_report_detail_required_exception_when_reason_is_etc_and_detail_is_blank() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.ETC, "   ");

        // when & then
        assertThatThrownBy(() -> reportService.create(REPORTER_ID, request))
                .isInstanceOf(ReportDetailRequiredException.class)
                .hasMessage(ErrorCode.REPORT_DETAIL_REQUIRED.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void should_create_report_when_reason_is_etc_and_detail_given_for_post_target() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.ETC, "스팸성 광고 게시글입니다");
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(REPORTER_ID, ReportTargetType.POST, POST_ID))
                .willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", REPORT_ID);
            return saved;
        });

        // when
        ReportCreateResponse response = reportService.create(REPORTER_ID, request);

        // then
        assertThat(response.id()).isEqualTo(REPORT_ID);
    }

    @Test
    void should_create_report_when_reason_is_not_etc_and_detail_is_null_for_comment_target() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.COMMENT, COMMENT_ID, ReportReason.ABUSE, null);
        given(commentRepository.existsById(COMMENT_ID)).willReturn(true);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(REPORTER_ID, ReportTargetType.COMMENT, COMMENT_ID))
                .willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", REPORT_ID);
            return saved;
        });

        // when
        ReportCreateResponse response = reportService.create(REPORTER_ID, request);

        // then
        assertThat(response.id()).isEqualTo(REPORT_ID);
    }

    @Test
    void should_throw_report_target_not_found_exception_when_target_type_is_post_and_post_does_not_exist() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(postRepository.existsById(POST_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reportService.create(REPORTER_ID, request))
                .isInstanceOf(ReportTargetNotFoundException.class)
                .hasMessage(ErrorCode.REPORT_TARGET_NOT_FOUND.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void should_throw_report_target_not_found_exception_when_target_type_is_comment_and_comment_does_not_exist() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.COMMENT, COMMENT_ID, ReportReason.SPAM, null);
        given(commentRepository.existsById(COMMENT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reportService.create(REPORTER_ID, request))
                .isInstanceOf(ReportTargetNotFoundException.class)
                .hasMessage(ErrorCode.REPORT_TARGET_NOT_FOUND.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void should_throw_report_already_exists_exception_when_reporter_already_reported_target() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(REPORTER_ID, ReportTargetType.POST, POST_ID))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> reportService.create(REPORTER_ID, request))
                .isInstanceOf(ReportAlreadyExistsException.class)
                .hasMessage(ErrorCode.REPORT_ALREADY_EXISTS.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void should_publish_report_created_event_when_report_created_successfully() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(REPORTER_ID, ReportTargetType.POST, POST_ID))
                .willReturn(false);
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", REPORT_ID);
            return saved;
        });

        // when
        reportService.create(REPORTER_ID, request);

        // then
        verify(eventPublisher).publishEvent(any(ReportCreatedEvent.class));
    }

    @Test
    void should_throw_report_already_exists_exception_when_save_violates_unique_constraint() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(postRepository.existsById(POST_ID)).willReturn(true);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(REPORTER_ID, ReportTargetType.POST, POST_ID))
                .willReturn(false);
        given(reportRepository.save(any(Report.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> reportService.create(REPORTER_ID, request))
                .isInstanceOf(ReportAlreadyExistsException.class)
                .hasMessage(ErrorCode.REPORT_ALREADY_EXISTS.getMessage());
        verify(eventPublisher, never()).publishEvent(any(ReportCreatedEvent.class));
    }
}
