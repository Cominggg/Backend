package com.Coming.Backend.notice.repository;

import com.Coming.Backend.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
