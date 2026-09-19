package com.Coming.Backend.notice.repository;

import com.Coming.Backend.notice.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE n.active = true ORDER BY n.createdAt DESC")
    List<Notice> findActiveNotices(Pageable pageable);
}
