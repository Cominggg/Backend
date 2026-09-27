package com.Coming.Backend.auth.service;

import com.Coming.Backend.auth.dto.MarketingUpdateRequest;
import com.Coming.Backend.auth.dto.MeResponse;
import com.Coming.Backend.auth.dto.NicknameCheckResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
import com.Coming.Backend.auth.dto.TokenPair;
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
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.entity.UserPolicyAgreement;
import com.Coming.Backend.policy.exception.PolicyNotFoundException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import com.Coming.Backend.policy.repository.UserPolicyAgreementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
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
    private final PolicyDocumentRepository policyDocumentRepository;
    private final UserPolicyAgreementRepository userPolicyAgreementRepository;

    /**
     * Refresh Token을 검증하고 새 Access Token과 새 Refresh Token을 발급한다.
     * 해당 세션(기기)의 Refresh Token만 교체되며, 기존 Refresh Token은 즉시 재사용이 불가능하다.
     *
     * @param refreshToken HttpOnly Cookie에서 추출한 Refresh Token
     */
    public TokenPair refreshToken(String refreshToken) {
        Long userId = extractUserIdFromRefreshToken(refreshToken);
        String sessionId = jwtProvider.getSessionId(refreshToken);
        if (sessionId == null) {
            // 세션 식별자(jti) 도입 이전에 발급된 토큰 — 재로그인 필요
            throw new RefreshTokenInvalidException();
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RefreshTokenInvalidException();
        }
        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, sessionId);
        if (!tokenRepository.rotate(userId, sessionId, refreshToken, newRefreshToken,
                jwtProvider.getRefreshTokenExpiry())) {
            throw new RefreshTokenInvalidException();
        }
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Access Token을 블랙리스트에 등록하고 현재 기기의 세션만 삭제한다.
     * Refresh Token이 없거나 유효하지 않으면 세션 삭제는 건너뛴다 (세션은 TTL 만료로 정리).
     *
     * @param accessToken  Authorization 헤더에서 추출한 Access Token
     * @param userId       인증된 사용자 ID
     * @param refreshToken HttpOnly Cookie에서 추출한 Refresh Token (nullable)
     */
    public void logout(String accessToken, Long userId, String refreshToken) {
        blacklistRepository.save(accessToken, jwtProvider.getRemainingExpiry(accessToken));
        findOwnSessionId(userId, refreshToken)
                .ifPresent(sessionId -> tokenRepository.deleteSession(userId, sessionId));
        log.info("로그아웃");
    }

    /**
     * 회원 탈퇴 처리 후 Access Token을 블랙리스트에 등록하고 모든 기기의 세션을 삭제한다.
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
        blacklistRepository.save(accessToken, jwtProvider.getRemainingExpiry(accessToken));
        tokenRepository.deleteAllSessions(userId);
        log.info("회원 탈퇴");
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
                Boolean.TRUE.equals(request.agreedMarketing())
        );
        try {
            recordPolicyAgreement(userId, PolicyType.TERMS);
            recordPolicyAgreement(userId, PolicyType.PRIVACY);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new NicknameDuplicateException();
        }
        String accessToken = jwtProvider.generateAccessToken(userId, user.getRole().name());
        log.info("회원가입 완료");
        return new TokenResponse(accessToken);
    }

    /**
     * 현재 시행 중인 정책 버전에 대한 사용자 동의 이력을 기록한다. 이미 동의 이력이 있으면 건너뛴다(탈퇴 후 재가입 대비).
     */
    private void recordPolicyAgreement(Long userId, PolicyType type) {
        PolicyDocument currentPolicy = policyDocumentRepository
                .findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(type, LocalDate.now())
                .orElseThrow(PolicyNotFoundException::new);
        if (userPolicyAgreementRepository.existsByUserIdAndPolicyId(userId, currentPolicy.getId())) {
            return;
        }
        userPolicyAgreementRepository.save(
                UserPolicyAgreement.builder()
                        .userId(userId)
                        .policyId(currentPolicy.getId())
                        .agreedAt(LocalDateTime.now())
                        .build()
        );
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
     * 마케팅 수신 동의 여부를 변경한다.
     */
    @Transactional
    public void updateMarketing(Long userId, MarketingUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.updateMarketing(request.agreedMarketing());
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

    // 다른 사용자의 Refresh Token으로 그 사용자의 세션을 지우지 못하도록 소유자를 확인한다.
    private Optional<String> findOwnSessionId(Long userId, String refreshToken) {
        if (refreshToken == null) {
            return Optional.empty();
        }
        try {
            if (!userId.equals(jwtProvider.getUserId(refreshToken))) {
                return Optional.empty();
            }
            return Optional.ofNullable(jwtProvider.getSessionId(refreshToken));
        } catch (ExpiredTokenException | InvalidTokenException e) {
            return Optional.empty();
        }
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(),
                user.getNickname(),
                user.getBirthYear(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getAgreedMarketing())
        );
    }
}
