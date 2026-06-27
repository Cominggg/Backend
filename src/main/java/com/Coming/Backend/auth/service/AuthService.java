package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.dto.MeResponse;
import com.Coming.Backend.auth.dto.NicknameCheckResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
import com.Coming.Backend.auth.dto.TokenResponse;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.exception.ExpiredTokenException;
import com.Coming.Backend.auth.exception.InvalidTokenException;
import com.Coming.Backend.auth.exception.BirthYearRequiredException;
import com.Coming.Backend.auth.exception.NicknameDuplicateException;
import com.Coming.Backend.auth.exception.NicknameRequiredException;
import com.Coming.Backend.auth.exception.NicknameTooLongException;
import com.Coming.Backend.auth.exception.RefreshTokenExpiredException;
import com.Coming.Backend.auth.exception.RefreshTokenInvalidException;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.TermsNotAgreedException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RefreshTokenInvalidException();
        }
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
        userRepository.flush();
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
     * 닉네임을 수정한다.
     */
    @Transactional
    public MeResponse updateMe(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (nickname != null && nickname.length() > 20) {
            throw new NicknameTooLongException();
        }
        if (nickname != null) {
            user.updateNickname(nickname);
        }
        return toMeResponse(user);
    }

    /**
     * 회원가입을 완료하고 USER 역할의 새 Access Token을 발급한다.
     */
    @Transactional
    public TokenResponse register(Long userId, RegisterRequest request) {
        validateRegisterRequest(request);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.completeRegistration(
                request.nickname(),
                request.birthYear(),
                request.agreedTerms(),
                request.agreedPrivacy(),
                Boolean.TRUE.equals(request.agreedMarketing())
        );
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new NicknameDuplicateException();
        }
        String accessToken = jwtProvider.generateAccessToken(userId, user.getRole().name());
        log.info("회원가입 완료 — userId: {}", userId);
        return new TokenResponse(accessToken);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.agreedTerms()) || !Boolean.TRUE.equals(request.agreedPrivacy())) {
            throw new TermsNotAgreedException();
        }
        if (request.nickname() == null || request.nickname().isBlank()) {
            throw new NicknameRequiredException();
        }
        if (request.nickname().length() > 20) {
            throw new NicknameTooLongException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new NicknameDuplicateException();
        }
        if (request.birthYear() == null) {
            throw new BirthYearRequiredException();
        }
    }

    /**
     * 닉네임 중복 여부를 확인한다.
     */
    public NicknameCheckResponse checkNickname(String nickname) {
        boolean available = !userRepository.existsByNickname(nickname);
        return new NicknameCheckResponse(available);
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
                user.getBirthYear(),
                user.getRole().name()
        );
    }
}
