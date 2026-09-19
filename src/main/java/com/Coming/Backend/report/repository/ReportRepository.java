package com.Coming.Backend.report.repository;

import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportStatus;
import com.Coming.Backend.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    Page<Report> findAllByTargetType(ReportTargetType targetType, Pageable pageable);

    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findAllByTargetTypeAndStatus(ReportTargetType targetType, ReportStatus status, Pageable pageable);
}
