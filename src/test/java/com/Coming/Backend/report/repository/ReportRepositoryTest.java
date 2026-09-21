package com.Coming.Backend.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    private static final Long REPORTER_ID = 1L;
    private static final Long OTHER_REPORTER_ID = 2L;
    private static final Long TARGET_ID = 10L;
    private static final Long OTHER_TARGET_ID = 20L;

    private Report buildReport(Long reporterId, ReportTargetType targetType, Long targetId) {
        return Report.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.PENDING)
                .build();
    }

    @Test
    void should_return_true_when_report_exists_with_same_reporter_target_type_and_target_id() {
        // given
        reportRepository.save(buildReport(REPORTER_ID, ReportTargetType.POST, TARGET_ID));

        // when
        boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                REPORTER_ID, ReportTargetType.POST, TARGET_ID);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void should_return_false_when_reporter_id_differs() {
        // given
        reportRepository.save(buildReport(REPORTER_ID, ReportTargetType.POST, TARGET_ID));

        // when
        boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                OTHER_REPORTER_ID, ReportTargetType.POST, TARGET_ID);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void should_return_false_when_target_type_differs() {
        // given
        reportRepository.save(buildReport(REPORTER_ID, ReportTargetType.POST, TARGET_ID));

        // when
        boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                REPORTER_ID, ReportTargetType.COMMENT, TARGET_ID);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void should_return_false_when_target_id_differs() {
        // given
        reportRepository.save(buildReport(REPORTER_ID, ReportTargetType.POST, TARGET_ID));

        // when
        boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                REPORTER_ID, ReportTargetType.POST, OTHER_TARGET_ID);

        // then
        assertThat(exists).isFalse();
    }
}
