package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.dto.MarketingUpdateRequest;
import com.Coming.Backend.auth.dto.MeResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
import com.Coming.Backend.auth.dto.TokenPair;
import com.Coming.Backend.auth.dto.TokenResponse;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.ExpiredTokenException;
import com.Coming.Backend.auth.exception.InvalidTokenException;
import com.Coming.Backend.auth.exception.NicknameDuplicateException;
import com.Coming.Backend.auth.exception.NicknameRequiredException;
import com.Coming.Backend.auth.exception.RefreshTokenExpiredException;
import com.Coming.Backend.auth.exception.RefreshTokenInvalidException;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import org.springframework.dao.DataIntegrityViolationException;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.jwt.RefreshTokenClaims;
import com.Coming.Backend.auth.repository.BlacklistRepository;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.TokenRepository.RotationResult;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.entity.UserPolicyAgreement;
import com.Coming.Backend.policy.exception.PolicyNotFoundException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import com.Coming.Backend.policy.repository.UserPolicyAgreementRepository;
import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private UserPolicyAgreementRepository userPolicyAgreementRepository;

    @Mock
    private WithdrawalService withdrawalService;

    @Mock
    private UserStatusValidator userStatusValidator;

    private static final Long USER_ID = 1L;
    private static final String REFRESH_TOKEN = "valid-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String SESSION_ID = "session-1";
    private static final long REFRESH_EXPIRY = 604_800_000L;
    private static final RefreshTokenClaims REFRESH_CLAIMS = new RefreshTokenClaims(USER_ID, SESSION_ID);

    private User buildUser() {
        return User.builder()
                .id(USER_ID)
                .nickname("테스터")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .provider("google")
                .providerId("google-123")
                .build();
    }

    private User buildPendingUser() {
        return User.builder()
                .id(USER_ID)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .provider("google")
                .providerId("google-123")
                .build();
    }

    private PolicyDocument buildPolicyDocument(Long policyId, PolicyType type) {
        return PolicyDocument.builder()
                .id(policyId)
                .type(type)
                .version("1.0")
                .effectiveDate(LocalDate.now())
                .changeSummary("최초 시행")
                .detailUrl("https://coming.example.com/policy")
                .build();
    }

    // -------------------------------------------------------------------------
    // createSession
    // -------------------------------------------------------------------------

    @Test
    void should_save_session_with_same_session_id_as_refresh_token_when_create_session() {
        // given
        given(jwtProvider.generateRefreshToken(eq(USER_ID), anyString())).willReturn(REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(REFRESH_EXPIRY);

        // when
        String result = authService.createSession(USER_ID);

        // then
        assertThat(result).isEqualTo(REFRESH_TOKEN);
        ArgumentCaptor<String> tokenSessionId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> savedSessionId = ArgumentCaptor.forClass(String.class);
        verify(jwtProvider).generateRefreshToken(eq(USER_ID), tokenSessionId.capture());
        verify(tokenRepository).saveSession(eq(USER_ID), savedSessionId.capture(), eq(REFRESH_TOKEN), eq(REFRESH_EXPIRY));
        assertThat(savedSessionId.getValue()).isEqualTo(tokenSessionId.getValue());
        assertThat(UUID.fromString(savedSessionId.getValue())).isNotNull();
    }

    @Test
    void should_issue_new_session_id_for_each_create_session() {
        // given
        given(jwtProvider.generateRefreshToken(eq(USER_ID), anyString())).willReturn(REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(REFRESH_EXPIRY);

        // when
        authService.createSession(USER_ID);
        authService.createSession(USER_ID);

        // then
        ArgumentCaptor<String> sessionIds = ArgumentCaptor.forClass(String.class);
        verify(jwtProvider, times(2)).generateRefreshToken(eq(USER_ID), sessionIds.capture());
        assertThat(sessionIds.getAllValues()).doesNotHaveDuplicates();
    }

    // -------------------------------------------------------------------------
    // issueTokens
    // -------------------------------------------------------------------------

    @Test
    void should_return_access_token_with_role_and_new_session_refresh_token_when_issue_tokens() {
        // given
        User user = buildPendingUser();
        given(jwtProvider.generateAccessToken(USER_ID, "PENDING")).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(eq(USER_ID), anyString())).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(REFRESH_EXPIRY);

        // when
        TokenPair result = authService.issueTokens(user);

        // then
        assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        verify(tokenRepository).saveSession(eq(USER_ID), anyString(), eq(NEW_REFRESH_TOKEN), eq(REFRESH_EXPIRY));
    }

    // -------------------------------------------------------------------------
    // refreshToken
    // -------------------------------------------------------------------------

    @Test
    void should_return_new_token_pair_and_rotate_refresh_token_when_valid() {
        // given
        User user = buildUser();
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(USER_ID, UserRole.USER.name())).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(USER_ID, SESSION_ID)).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(REFRESH_EXPIRY);
        given(tokenRepository.rotate(USER_ID, SESSION_ID, REFRESH_TOKEN, NEW_REFRESH_TOKEN, REFRESH_EXPIRY))
                .willReturn(RotationResult.ROTATED);

        // when
        TokenPair result = authService.refreshToken(REFRESH_TOKEN);

        // then
        assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        verify(tokenRepository).rotate(USER_ID, SESSION_ID, REFRESH_TOKEN, NEW_REFRESH_TOKEN, REFRESH_EXPIRY);
    }

    @Test
    void should_throw_refresh_token_expired_exception_when_refresh_token_is_expired() {
        // given
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willThrow(new ExpiredTokenException());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenExpiredException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
    }

    @Test
    void should_throw_RefreshTokenInvalidException_when_token_is_not_a_valid_refresh_token() {
        // given — Access Token 또는 jti·typ 도입 이전에 발급된 Refresh Token
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willThrow(new InvalidTokenException());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
        verify(tokenRepository, never()).rotate(any(), any(), any(), any(), anyLong());
    }

    @Test
    void should_throw_RefreshTokenInvalidException_when_inactive_user_refreshes_token() {
        // given
        User inactiveUser = User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .provider("google")
                .providerId("google-123")
                .build();
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_UserSuspendedException_with_period_and_reason_when_suspended_user_refreshes_token() {
        // given
        User suspendedUser = buildUser();
        LocalDateTime until = LocalDateTime.now().plusDays(7);
        suspendedUser.suspend(until, ReportReason.ABUSE, "욕설");
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(UserSuspendedException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage())
                .satisfies(e -> {
                    UserSuspendedException ex = (UserSuspendedException) e;
                    assertThat(ex.getSuspendedUntil()).isEqualTo(until);
                    assertThat(ex.getReason()).isEqualTo(ReportReason.ABUSE);
                });
        verify(tokenRepository, never()).rotate(any(), any(), any(), any(), anyLong());
    }

    @Test
    void should_throw_RefreshTokenInvalidException_when_suspension_has_expired_but_not_yet_released() {
        // given
        User suspendedUser = buildUser();
        suspendedUser.suspend(LocalDateTime.now().minusMinutes(1), ReportReason.SPAM, null);
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    void should_throw_RefreshTokenInvalidException_when_reused_refresh_token_is_detected() {
        // given — 이미 회전된 토큰 재제출 (세션은 저장소에서 폐기됨)
        User user = buildUser();
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(USER_ID, UserRole.USER.name())).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(USER_ID, SESSION_ID)).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(REFRESH_EXPIRY);
        given(tokenRepository.rotate(USER_ID, SESSION_ID, REFRESH_TOKEN, NEW_REFRESH_TOKEN, REFRESH_EXPIRY))
                .willReturn(RotationResult.REUSE_DETECTED);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_RefreshTokenInvalidException_when_session_does_not_exist() {
        // given — 로그아웃·세션 상한 초과·탈퇴로 제거된 세션
        User user = buildUser();
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(USER_ID, UserRole.USER.name())).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(USER_ID, SESSION_ID)).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(REFRESH_EXPIRY);
        given(tokenRepository.rotate(USER_ID, SESSION_ID, REFRESH_TOKEN, NEW_REFRESH_TOKEN, REFRESH_EXPIRY))
                .willReturn(RotationResult.SESSION_NOT_FOUND);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    void should_register_blacklist_and_delete_current_session_when_logout() {
        // given
        long remainingExpiry = 900_000L;
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(remainingExpiry);
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(REFRESH_CLAIMS);

        // when
        authService.logout(ACCESS_TOKEN, USER_ID, REFRESH_TOKEN);

        // then
        verify(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
        verify(tokenRepository).deleteSession(USER_ID, SESSION_ID);
        verify(tokenRepository, never()).deleteAllSessions(any());
    }

    @Test
    void should_only_register_blacklist_when_logout_without_refresh_token() {
        // given
        long remainingExpiry = 900_000L;
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(remainingExpiry);

        // when
        authService.logout(ACCESS_TOKEN, USER_ID, null);

        // then
        verify(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
        verify(tokenRepository, never()).deleteSession(any(), any());
    }

    @Test
    void should_not_delete_session_when_logout_with_invalid_refresh_token() {
        // given
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(900_000L);
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willThrow(new InvalidTokenException());

        // when
        authService.logout(ACCESS_TOKEN, USER_ID, REFRESH_TOKEN);

        // then
        verify(tokenRepository, never()).deleteSession(any(), any());
    }

    @Test
    void should_not_delete_session_when_logout_with_other_users_refresh_token() {
        // given
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(900_000L);
        given(jwtProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(new RefreshTokenClaims(2L, SESSION_ID));

        // when
        authService.logout(ACCESS_TOKEN, USER_ID, REFRESH_TOKEN);

        // then
        verify(tokenRepository, never()).deleteSession(any(), any());
    }

    // -------------------------------------------------------------------------
    // withdraw
    // -------------------------------------------------------------------------

    @Test
    void should_delegate_withdrawal_and_blacklist_access_token_when_withdraw() {
        // given
        long remainingExpiry = 900_000L;
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(remainingExpiry);

        // when
        authService.withdraw(ACCESS_TOKEN, USER_ID);

        // then
        verify(withdrawalService).withdraw(USER_ID);
        verify(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
    }

    @Test
    void should_throw_UserSuspendedException_and_not_withdraw_when_suspended_user_withdraws() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(USER_ID);

        // when & then
        assertThatThrownBy(() -> authService.withdraw(ACCESS_TOKEN, USER_ID))
                .isInstanceOf(UserSuspendedException.class);
        verify(withdrawalService, never()).withdraw(any());
        verify(blacklistRepository, never()).save(any(), anyLong());
    }

    // -------------------------------------------------------------------------
    // getMe
    // -------------------------------------------------------------------------

    @Test
    void should_throw_user_not_found_exception_when_user_does_not_exist() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.getMe(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // getMe
    // -------------------------------------------------------------------------

    @Test
    void should_return_agreed_marketing_true_when_user_agreed_marketing() {
        // given
        User user = User.builder()
                .id(USER_ID).nickname("테스터").role(UserRole.USER).status(UserStatus.ACTIVE)
                .provider("google").providerId("google-123").agreedMarketing(true).build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        MeResponse response = authService.getMe(USER_ID);

        // then
        assertThat(response.agreedMarketing()).isTrue();
    }

    @Test
    void should_return_agreed_marketing_false_when_user_has_null_agreed_marketing() {
        // given
        User user = buildUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        MeResponse response = authService.getMe(USER_ID);

        // then
        assertThat(response.agreedMarketing()).isFalse();
    }

    // -------------------------------------------------------------------------
    // updateMarketing
    // -------------------------------------------------------------------------

    @Test
    void should_set_agreed_marketing_true_when_update_marketing_with_true() {
        // given
        User user = buildUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        authService.updateMarketing(USER_ID, new MarketingUpdateRequest(true));

        // then
        assertThat(user.getAgreedMarketing()).isTrue();
    }

    @Test
    void should_set_agreed_marketing_false_when_update_marketing_with_false() {
        // given
        User user = User.builder()
                .id(USER_ID).nickname("테스터").role(UserRole.USER).status(UserStatus.ACTIVE)
                .provider("google").providerId("google-123").agreedMarketing(true).build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        authService.updateMarketing(USER_ID, new MarketingUpdateRequest(false));

        // then
        assertThat(user.getAgreedMarketing()).isFalse();
    }

    @Test
    void should_throw_user_not_found_when_update_marketing_with_invalid_user() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.updateMarketing(USER_ID, new MarketingUpdateRequest(true)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void should_throw_UserNotFoundException_and_not_update_marketing_when_withdrawn_user_updates_marketing() {
        // given
        willThrow(new UserNotFoundException()).given(userStatusValidator).validateWritable(USER_ID);

        // when & then
        assertThatThrownBy(() -> authService.updateMarketing(USER_ID, new MarketingUpdateRequest(true)))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).findById(any());
    }

    // -------------------------------------------------------------------------
    // updateMe
    // -------------------------------------------------------------------------

    @Test
    void should_return_me_response_with_updated_nickname_when_nickname_is_given() {
        // given
        User user = buildUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        MeResponse response = authService.updateMe(USER_ID, "새닉네임");

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.role()).isEqualTo(UserRole.USER.name());
    }

    @Test
    void should_throw_NicknameDuplicateException_when_updating_to_nickname_used_by_other_user() {
        // given
        User user = buildUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.updateMe(USER_ID, "중복닉"))
                .isInstanceOf(NicknameDuplicateException.class);
        assertThat(user.getNickname()).isEqualTo("테스터");
    }

    @Test
    void should_keep_nickname_without_duplicate_check_when_updating_to_same_nickname() {
        // given
        User user = buildUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        MeResponse response = authService.updateMe(USER_ID, "테스터");

        // then
        assertThat(response.nickname()).isEqualTo("테스터");
        verify(userRepository, never()).existsByNickname(any());
    }

    @Test
    void should_throw_NicknameRequiredException_when_nickname_is_blank() {
        // given
        User user = buildUser();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.updateMe(USER_ID, "  "))
                .isInstanceOf(NicknameRequiredException.class);
        assertThat(user.getNickname()).isEqualTo("테스터");
    }

    @Test
    void should_throw_UserSuspendedException_and_not_update_nickname_when_suspended_user_updates_me() {
        // given
        willThrow(new UserSuspendedException(null, ReportReason.SPAM)).given(userStatusValidator).validateWritable(USER_ID);

        // when & then
        assertThatThrownBy(() -> authService.updateMe(USER_ID, "새닉네임"))
                .isInstanceOf(UserSuspendedException.class);
        verify(userRepository, never()).findById(any());
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void should_throw_NicknameDuplicateException_when_nickname_conflict_occurs_on_register() {
        // given
        User user = buildPendingUser();
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("IU")).willReturn(false);
        given(policyDocumentRepository.findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                any(PolicyType.class), any(LocalDate.class)))
                .willReturn(Optional.of(buildPolicyDocument(10L, PolicyType.TERMS)));
        willThrow(DataIntegrityViolationException.class).given(userRepository).flush();

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(NicknameDuplicateException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATE.getMessage());
    }

    @Test
    void should_save_user_policy_agreement_for_terms_and_privacy_when_register_succeeds() {
        // given
        User user = buildPendingUser();
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        PolicyDocument termsPolicy = buildPolicyDocument(10L, PolicyType.TERMS);
        PolicyDocument privacyPolicy = buildPolicyDocument(20L, PolicyType.PRIVACY);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("IU")).willReturn(false);
        given(policyDocumentRepository.findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq(PolicyType.TERMS), any(LocalDate.class)))
                .willReturn(Optional.of(termsPolicy));
        given(policyDocumentRepository.findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq(PolicyType.PRIVACY), any(LocalDate.class)))
                .willReturn(Optional.of(privacyPolicy));

        // when
        authService.register(USER_ID, request);

        // then
        ArgumentCaptor<UserPolicyAgreement> captor = ArgumentCaptor.forClass(UserPolicyAgreement.class);
        verify(userPolicyAgreementRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(UserPolicyAgreement::getUserId, UserPolicyAgreement::getPolicyId)
                .containsExactlyInAnyOrder(tuple(USER_ID, 10L), tuple(USER_ID, 20L));
    }

    @Test
    void should_not_save_user_policy_agreement_when_agreement_already_exists() {
        // given
        User user = buildPendingUser();
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        PolicyDocument termsPolicy = buildPolicyDocument(10L, PolicyType.TERMS);
        PolicyDocument privacyPolicy = buildPolicyDocument(20L, PolicyType.PRIVACY);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("IU")).willReturn(false);
        given(policyDocumentRepository.findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq(PolicyType.TERMS), any(LocalDate.class)))
                .willReturn(Optional.of(termsPolicy));
        given(policyDocumentRepository.findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq(PolicyType.PRIVACY), any(LocalDate.class)))
                .willReturn(Optional.of(privacyPolicy));
        given(userPolicyAgreementRepository.existsByUserIdAndPolicyId(USER_ID, 10L)).willReturn(true);
        given(userPolicyAgreementRepository.existsByUserIdAndPolicyId(USER_ID, 20L)).willReturn(true);

        // when
        authService.register(USER_ID, request);

        // then
        verify(userPolicyAgreementRepository, never()).save(any());
    }

    @Test
    void should_throw_PolicyNotFoundException_when_no_current_policy_exists() {
        // given
        User user = buildPendingUser();
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("IU")).willReturn(false);
        given(policyDocumentRepository.findFirstByTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq(PolicyType.TERMS), any(LocalDate.class)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessage(ErrorCode.POLICY_NOT_FOUND.getMessage());
    }

    @Test
    void should_throw_UserNotFoundException_and_not_register_when_withdrawn_user_registers() {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        willThrow(new UserNotFoundException()).given(userStatusValidator).validateWritable(USER_ID);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).findById(any());
        verify(userPolicyAgreementRepository, never()).save(any());
        verify(userRepository, never()).flush();
        verify(jwtProvider, never()).generateAccessToken(anyLong(), anyString());
    }
}
