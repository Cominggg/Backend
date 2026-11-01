package com.Coming.Backend.auth.service;

import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.event.AppleTokenRevokeEvent;
import com.Coming.Backend.auth.event.KakaoUnlinkEvent;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import com.Coming.Backend.rating.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 처리. 본인 탈퇴와 관리자 대리 탈퇴가 같은 로직을 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private static final String KAKAO = "kakao";

    private final UserRepository userRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;
    private final UserConcertCalendarRepository userConcertCalendarRepository;
    private final InquiryRepository inquiryRepository;
    private final RatingRepository ratingRepository;
    private final TokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원의 팔로우·캘린더·문의·별점을 삭제하고 개인정보를 파기한 뒤 모든 기기의 세션을 삭제한다.
     * 게시글·댓글은 남기고 작성자 닉네임만 비운다.
     * Apple·Kakao 회원이면 커밋 후 연결 끊기를 요청한다(외부 호출을 트랜잭션 밖으로 분리, 실패해도 탈퇴는 유지).
     *
     * @throws UserNotFoundException 존재하지 않거나 이미 탈퇴한 회원인 경우
     */
    @Transactional
    public void withdraw(Long userId) {
        userFollowArtistRepository.deleteByUserId(userId);
        userConcertCalendarRepository.deleteByUserId(userId);
        inquiryRepository.deleteByUserId(userId);
        ratingRepository.deleteByUserId(userId);

        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() != UserStatus.INACTIVE)
                .orElseThrow(UserNotFoundException::new);
        requestAppleTokenRevoke(user);
        requestKakaoUnlink(user);
        user.withdraw();
        userRepository.flush();
        tokenRepository.deleteAllSessions(userId);
        log.info("회원 탈퇴 — userId: {}", userId);
    }

    private void requestAppleTokenRevoke(User user) {
        if (user.getAppleRefreshToken() != null) {
            eventPublisher.publishEvent(new AppleTokenRevokeEvent(user.getId(), user.getAppleRefreshToken()));
        }
    }

    /**
     * withdraw()가 provider_id를 바꾸기 전에 Kakao 회원번호를 이벤트에 담는다.
     */
    private void requestKakaoUnlink(User user) {
        if (KAKAO.equals(user.getProvider())) {
            eventPublisher.publishEvent(new KakaoUnlinkEvent(user.getId(), user.getProviderId()));
        }
    }
}
