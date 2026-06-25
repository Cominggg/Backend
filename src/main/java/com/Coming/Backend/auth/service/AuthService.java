package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.dto.MeResponse;
import com.Coming.Backend.auth.dto.TokenResponse;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.exception.ExpiredTokenException;
import com.Coming.Backend.auth.exception.InvalidFileTypeException;
import com.Coming.Backend.auth.exception.InvalidTokenException;
import com.Coming.Backend.auth.exception.NicknameTooLongException;
import com.Coming.Backend.auth.exception.RefreshTokenExpiredException;
import com.Coming.Backend.auth.exception.RefreshTokenInvalidException;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.auth.repository.BlacklistRepository;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final TokenRepository tokenRepository;
    private final BlacklistRepository blacklistRepository;
    private final UserRepository userRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;
    private final UserConcertCalendarRepository userConcertCalendarRepository;
    private final InquiryRepository inquiryRepository;

    /**
     * Refresh Token을 검증하고 새 Access Token을 발급한다.
     *
     * @param refreshToken HttpOnly Cookie에서 추출한 Refresh Token
     */
    public TokenResponse refreshToken(String refreshToken) {
        Long userId = extractUserIdFromRefreshToken(refreshToken);
        validateStoredRefreshToken(userId, refreshToken);

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getRole().name());
        return new TokenResponse(newAccessToken);
    }

    /**
     * Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제한다.
     *
     * @param accessToken Authorization 헤더에서 추출한 Access Token
     * @param userId      인증된 사용자 ID
     */
    public void logout(String accessToken, Long userId) {
        blacklistRepository.save(accessToken, jwtProvider.getRemainingExpiry(accessToken));
        tokenRepository.delete(userId);
        log.info("로그아웃 — userId: {}", userId);
    }

    /**
     * 회원 탈퇴 처리 후 토큰을 무효화한다.
     *
     * @param accessToken Authorization 헤더에서 추출한 Access Token
     * @param userId      인증된 사용자 ID
     */
    @Transactional
    public void withdraw(String accessToken, Long userId) {
        userFollowArtistRepository.deleteByUserId(userId);
        userConcertCalendarRepository.deleteByUserId(userId);
        inquiryRepository.deleteByUserId(userId);

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.withdraw();
        userRepository.flush();  // DB 반영 확인 후 Redis 쓰기 — 역순 부분 실패 방지
        logout(accessToken, userId);
        log.info("회원 탈퇴 — userId: {}", userId);
    }

    /**
     * 로그인한 사용자의 프로필 정보를 조회한다.
     */
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return toMeResponse(user);
    }

    /**
     * 닉네임을 수정한다. 프로필 이미지 파일이 전달되면 INVALID_FILE_TYPE을 반환한다.
     */
    @Transactional
    public MeResponse updateMe(Long userId, String nickname, MultipartFile profileImage) {
        if (profileImage != null && !profileImage.isEmpty()) {
            throw new InvalidFileTypeException();
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (nickname != null && nickname.length() > 20) {
            throw new NicknameTooLongException();
        }
        if (nickname != null) {
            user.updateNickname(nickname);
        }
        return toMeResponse(user);
    }

    private Long extractUserIdFromRefreshToken(String refreshToken) {
        try {
            return jwtProvider.getUserId(refreshToken);
        } catch (ExpiredTokenException e) {
            throw new RefreshTokenExpiredException();
        } catch (InvalidTokenException e) {
            throw new RefreshTokenInvalidException();
        }
    }

    private void validateStoredRefreshToken(Long userId, String refreshToken) {
        String stored = tokenRepository.find(userId)
                .orElseThrow(RefreshTokenInvalidException::new);
        if (!stored.equals(refreshToken)) {
            throw new RefreshTokenInvalidException();
        }
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(),
                user.getNickname(),
                user.getRole().name()
        );
    }
}
