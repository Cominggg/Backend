package com.Coming.Backend.inquiry.repository;

import com.Coming.Backend.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
