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

    boolean existsByUserIdAndTargetIdAndTypeAndStatus(Long userId, Long targetId, InquiryType type, InquiryStatus status);

    void deleteByUserId(Long userId);

    Page<Inquiry> findAllByType(InquiryType type, Pageable pageable);

    Page<Inquiry> findAllByStatus(InquiryStatus status, Pageable pageable);

    Page<Inquiry> findAllByTypeAndStatus(InquiryType type, InquiryStatus status, Pageable pageable);
}
