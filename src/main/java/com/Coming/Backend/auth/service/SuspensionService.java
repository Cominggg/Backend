package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.dto.SuspensionCommand;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.event.SuspensionNoticeEvent;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 정지·해제. 정지는 관리자가 신고 처리와 함께 또는 단독으로 실행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuspensionService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원을 정지하고 모든 기기의 세션을 삭제한다. 이미 정지 중이면 새 기간·사유로 덮어쓴다.
     * 이메일이 있으면 커밋 후 정지 통지 메일을 보낸다.
     * 정지 직후 남은 Access Token(최대 30분)은 읽기만 허용되고 쓰기는 회원 상태 확인으로 막힌다.
     *
     * @throws UserNotFoundException 존재하지 않거나 탈퇴한 회원인 경우
     */
    @Transactional
    public void suspend(Long userId, SuspensionCommand command) {
        User user = findNotWithdrawnUser(userId);
        LocalDateTime until = command.duration().untilFrom(LocalDateTime.now());
        user.suspend(until, command.reason(), command.detail());
        tokenRepository.deleteAllSessions(userId);
        if (!user.hasNoEmail()) {
            eventPublisher.publishEvent(
                    new SuspensionNoticeEvent(userId, user.getEmail(), until, command.reason(), command.detail()));
        }
        log.info("회원 정지 — userId: {}, until: {}, reason: {}", userId, until, command.reason());
    }

    /**
     * 회원 정지를 해제한다. 정지 상태가 아니면 아무것도 하지 않는다.
     *
     * @throws UserNotFoundException 존재하지 않거나 탈퇴한 회원인 경우
     */
    @Transactional
    public void unsuspend(Long userId) {
        User user = findNotWithdrawnUser(userId);
        if (user.getStatus() == UserStatus.SUSPENDED) {
            user.unsuspend();
            log.info("회원 정지 해제 — userId: {}", userId);
        }
    }

    private User findNotWithdrawnUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() != UserStatus.INACTIVE)
                .orElseThrow(UserNotFoundException::new);
    }
}
