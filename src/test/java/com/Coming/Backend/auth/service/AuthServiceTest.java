package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
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
import com.Coming.Backend.auth.exception.NicknameDuplicateException;
import com.Coming.Backend.auth.exception.RefreshTokenExpiredException;
import com.Coming.Backend.auth.exception.RefreshTokenInvalidException;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.auth.repository.BlacklistRepository;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.calendar.repository.UserConcertCalendarRepository;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.inquiry.repository.InquiryRepository;
import com.Coming.Backend.policy.entity.PolicyDocument;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.entity.UserPolicyAgreement;
import com.Coming.Backend.policy.exception.PolicyNotFoundException;
import com.Coming.Backend.policy.repository.PolicyDocumentRepository;
import com.Coming.Backend.policy.repository.UserPolicyAgreementRepository;
import java.time.LocalDate;
import java.util.Optional;
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
    private UserFollowArtistRepository userFollowArtistRepository;

    @Mock
    private UserConcertCalendarRepository userConcertCalendarRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private PolicyDocumentRepository policyDocumentRepository;

    @Mock
    private UserPolicyAgreementRepository userPolicyAgreementRepository;

    private static final Long USER_ID = 1L;
    private static final String REFRESH_TOKEN = "valid-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";

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
    // refreshToken
    // -------------------------------------------------------------------------

    @Test
    void should_return_new_token_pair_and_rotate_refresh_token_when_valid() {
        // given
        User user = buildUser();
        given(jwtProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
        given(tokenRepository.find(USER_ID)).willReturn(Optional.of(REFRESH_TOKEN));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(USER_ID, UserRole.USER.name())).willReturn(NEW_ACCESS_TOKEN);
        given(jwtProvider.generateRefreshToken(USER_ID)).willReturn(NEW_REFRESH_TOKEN);
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(604800000L);

        // when
        TokenPair result = authService.refreshToken(REFRESH_TOKEN);

        // then
        assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        verify(tokenRepository).save(USER_ID, NEW_REFRESH_TOKEN, 604800000L);
    }

    @Test
    void should_throw_refresh_token_expired_exception_when_refresh_token_is_expired() {
        // given
        given(jwtProvider.getUserId(REFRESH_TOKEN)).willThrow(new ExpiredTokenException());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenExpiredException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
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
        given(jwtProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
        given(tokenRepository.find(USER_ID)).willReturn(Optional.of(REFRESH_TOKEN));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_RefreshTokenInvalidException_when_suspended_user_refreshes_token() {
        // given
        User suspendedUser = User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.SUSPENDED)
                .provider("google")
                .providerId("google-123")
                .build();
        given(jwtProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
        given(tokenRepository.find(USER_ID)).willReturn(Optional.of(REFRESH_TOKEN));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    void should_throw_refresh_token_invalid_exception_when_refresh_token_does_not_match_redis() {
        // given
        given(jwtProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
        given(tokenRepository.find(USER_ID)).willReturn(Optional.of("different-token"));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    void should_register_blacklist_and_delete_refresh_token_when_logout() {
        // given
        long remainingExpiry = 900_000L;
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(remainingExpiry);
        willDoNothing().given(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
        willDoNothing().given(tokenRepository).delete(USER_ID);

        // when
        authService.logout(ACCESS_TOKEN, USER_ID);

        // then
        verify(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
        verify(tokenRepository).delete(USER_ID);
    }

    // -------------------------------------------------------------------------
    // withdraw
    // -------------------------------------------------------------------------

    @Test
    void should_call_withdraw_and_invalidate_tokens_when_withdraw() {
        // given
        User user = buildUser();
        long remainingExpiry = 900_000L;
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.getRemainingExpiry(ACCESS_TOKEN)).willReturn(remainingExpiry);
        willDoNothing().given(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
        willDoNothing().given(tokenRepository).delete(USER_ID);

        // when
        authService.withdraw(ACCESS_TOKEN, USER_ID);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.getNickname()).isNull();
        verify(userFollowArtistRepository).deleteByUserId(USER_ID);
        verify(userConcertCalendarRepository).deleteByUserId(USER_ID);
        verify(inquiryRepository).deleteByUserId(USER_ID);
        verify(blacklistRepository).save(ACCESS_TOKEN, remainingExpiry);
        verify(tokenRepository).delete(USER_ID);
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
}
