package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.dto.AdminArtistCreateRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.inquiry.entity.Inquiry;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ArtistRepository artistRepository;
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 아티스트를 수동 등록한다.
     */
    @Transactional
    public void createArtist(AdminArtistCreateRequest request) {
        artistRepository.save(Artist.builder()
                .mbid(request.mbid())
                .name(request.name())
                .sortName(request.sortName())
                .debutDate(request.debutDate())
                .isComing(false)
                .build());
    }

    /**
     * 아티스트 정보를 수정한다. 존재하지 않는 아티스트 ID이면 ArtistNotFoundException을 던진다.
     */
    @Transactional
    public void updateArtist(Long id, AdminArtistUpdateRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(ArtistNotFoundException::new);
        artist.update(request.name(), request.sortName(), request.debutDate());
    }

    /**
     * 전체 문의 목록을 조회한다. type·status 중 null인 항목은 필터 없이 조회한다.
     */
    public PageResponse<AdminInquiryListItemResponse> getInquiries(InquiryType type, InquiryStatus status, Pageable pageable) {
        Page<Inquiry> page;
        if (type != null && status != null) {
            page = inquiryRepository.findAllByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            page = inquiryRepository.findAllByType(type, pageable);
        } else if (status != null) {
            page = inquiryRepository.findAllByStatus(status, pageable);
        } else {
            page = inquiryRepository.findAll(pageable);
        }

        List<Long> userIds = page.getContent().stream()
                .map(Inquiry::getUserId)
                .distinct()
                .toList();
        Map<Long, String> nicknameByUserId = userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return PageResponse.from(page.map(inquiry ->
                AdminInquiryListItemResponse.of(inquiry, nicknameByUserId.getOrDefault(inquiry.getUserId(), ""))));
    }

    /**
     * 문의 상세를 조회한다. 존재하지 않는 ID이면 InquiryNotFoundException을 던진다.
     */
    public AdminInquiryDetailResponse getInquiryDetail(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(InquiryNotFoundException::new);
        String nickname = userRepository.findById(inquiry.getUserId())
                .map(User::getNickname)
                .orElse("");
        return AdminInquiryDetailResponse.of(inquiry, nickname);
    }

    /**
     * 문의 처리 상태를 변경한다. 존재하지 않는 ID이면 InquiryNotFoundException을 던진다.
     */
    @Transactional
    public void updateInquiryStatus(Long id, AdminInquiryStatusUpdateRequest request) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(InquiryNotFoundException::new);
        inquiry.updateStatus(request.status(), request.adminNote());
    }
}
