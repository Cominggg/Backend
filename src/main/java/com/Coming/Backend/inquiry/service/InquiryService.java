package com.Coming.Backend.inquiry.service;

import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryListItemResponse;
import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryAlreadyPendingException;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.Coming.Backend.inquiry.exception.TargetNotFoundException;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final ConcertRepository concertRepository;
    private final ArtistRepository artistRepository;

    /**
     * 데이터 문의를 등록한다. 동일 targetId·type으로 PENDING 문의가 존재하면 예외를 던진다.
     */
    @Transactional
    public void createInquiry(Long userId, InquiryCreateRequest request) {
        validateTargetExists(request.type(), request.targetId());

        if (inquiryRepository.existsByTargetIdAndTypeAndStatus(
                request.targetId(), request.type(), InquiryStatus.PENDING)) {
            throw new InquiryAlreadyPendingException();
        }

        Inquiry inquiry = Inquiry.builder()
                .userId(userId)
                .type(request.type())
                .targetId(request.targetId())
                .title(request.title())
                .content(request.content())
                .status(InquiryStatus.PENDING)
                .build();

        inquiryRepository.save(inquiry);
    }

    /**
     * 내 문의 목록을 조회한다.
     *
     * @param status null이면 전체 조회
     */
    public PageResponse<InquiryListItemResponse> getMyInquiries(Long userId, InquiryStatus status, Pageable pageable) {
        Page<Inquiry> page = (status == null)
                ? inquiryRepository.findByUserId(userId, pageable)
                : inquiryRepository.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.from(page.map(InquiryListItemResponse::from));
    }

    /**
     * 내 문의 상세를 조회한다. 타인의 문의에 접근하면 예외를 던진다.
     */
    public InquiryDetailResponse getMyInquiryDetail(Long userId, Long id) {
        Inquiry inquiry = inquiryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(InquiryNotFoundException::new);
        return InquiryDetailResponse.from(inquiry);
    }

    private void validateTargetExists(InquiryType type, Long targetId) {
        boolean exists = switch (type) {
            case CONCERT, SETLIST -> concertRepository.existsById(targetId);
            case ARTIST -> artistRepository.existsById(targetId);
        };
        if (!exists) {
            throw new TargetNotFoundException();
        }
    }
}
