package com.Coming.Backend.report.repository;

import com.Coming.Backend.report.entity.Report;
import com.Coming.Backend.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);
}
