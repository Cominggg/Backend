package com.Coming.Backend.inquiry.service;

import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.inquiry.dto.InquiryCreateRequest;
import com.Coming.Backend.inquiry.dto.InquiryDetailResponse;
import com.Coming.Backend.inquiry.dto.InquiryExistsResponse;
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
     * 문의를 등록한다. CONCERT·ARTIST·SETLIST 타입은 동일 targetId·type으로 PENDING 문의가 존재하면 예외를 던진다.
     * DATA_REQUEST·FEEDBACK 타입은 대상 컨텐츠가 없으므로 targetId 검증 및 중복 체크를 수행하지 않는다.
     */
    @Transactional
    public void createInquiry(Long userId, InquiryCreateRequest request) {
        boolean isTargetless = isTargetlessType(request.type());

        if (!isTargetless) {
            validateTargetExists(request.type(), request.targetId());
            if (inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(
                    userId, request.targetId(), request.type(), InquiryStatus.PENDING)) {
                throw new InquiryAlreadyPendingException();
            }
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

    /**
     * 해당 type·targetId 조합으로 PENDING 상태 문의가 존재하는지 조회한다.
     * DATA_REQUEST·FEEDBACK 타입은 중복 개념이 없으므로 항상 false를 반환한다.
     */
    public InquiryExistsResponse existsPendingInquiry(Long userId, InquiryType type, Long targetId) {
        if (isTargetlessType(type)) {
            return new InquiryExistsResponse(false);
        }
        boolean exists = inquiryRepository.existsByUserIdAndTargetIdAndTypeAndStatus(
                userId, targetId, type, InquiryStatus.PENDING);
        return new InquiryExistsResponse(exists);
    }

    private boolean isTargetlessType(InquiryType type) {
        return type == InquiryType.DATA_REQUEST || type == InquiryType.FEEDBACK;
    }

    private void validateTargetExists(InquiryType type, Long targetId) {
        boolean exists = switch (type) {
            case CONCERT, SETLIST -> concertRepository.existsById(targetId);
            case ARTIST -> artistRepository.existsById(targetId);
            default -> false;
        };
        if (!exists) {
            throw new TargetNotFoundException();
        }
    }
}
