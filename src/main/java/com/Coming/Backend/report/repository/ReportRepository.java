package com.Coming.Backend.report.repository;

import com.Coming.Backend.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
