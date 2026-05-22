package com.Coming.Backend.inquiry.repository;

import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByUserId(Long userId, Pageable pageable);

    Page<Inquiry> findByUserIdAndStatus(Long userId, InquiryStatus status, Pageable pageable);

    Optional<Inquiry> findByIdAndUserId(Long id, Long userId);

    boolean existsByTargetIdAndTypeAndStatus(Long targetId, InquiryType type, InquiryStatus status);

    void deleteByUserId(Long userId);
}
