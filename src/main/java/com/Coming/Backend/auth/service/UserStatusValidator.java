package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 쓰기 API 진입 시 회원 상태를 확인한다. 정지·탈퇴 직후에도 Access Token은 만료(최대 30분)까지 유효하므로
 * 토큰만으로는 막을 수 없는 쓰기를 DB 상태로 차단한다.
 */
@Service
@RequiredArgsConstructor
public class UserStatusValidator {

    private final UserRepository userRepository;

    /**
     * 쓰기·수정·삭제가 가능한 회원인지 확인한다. 정지 회원은 비회원처럼 조회만 할 수 있다.
     * 정지 기간이 지난 회원은 다음 로그인 때 해제되므로 허용한다.
     *
     * @throws UserSuspendedException 정지 중인 회원인 경우
     * @throws UserNotFoundException  존재하지 않거나 탈퇴한 회원인 경우
     */
    public void validateWritable(Long userId) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() != UserStatus.INACTIVE)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() == UserStatus.SUSPENDED && !user.isSuspensionExpired()) {
            throw new UserSuspendedException(user.getSuspendedUntil(), user.getSuspensionReason());
        }
    }
}
