package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.dto.MarketingUpdateRequest;
import com.Coming.Backend.auth.dto.MeResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private static final Long USER_ID = 1L;
    private static final String REFRESH_TOKEN = "valid-refresh-token";
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

    // -------------------------------------------------------------------------
    // refreshToken
    // -------------------------------------------------------------------------

    @Test
    void should_return_new_access_token_when_refresh_token_is_valid_and_matches_redis() {
        // given
        User user = buildUser();
        given(jwtProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
        given(tokenRepository.find(USER_ID)).willReturn(Optional.of(REFRESH_TOKEN));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(USER_ID, UserRole.USER.name())).willReturn(NEW_ACCESS_TOKEN);

        // when
        TokenResponse response = authService.refreshToken(REFRESH_TOKEN);

        // then
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
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
        User user = User.builder()
                .id(USER_ID)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .provider("google")
                .providerId("google-123")
                .build();
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("IU")).willReturn(false);
        willThrow(DataIntegrityViolationException.class).given(userRepository).flush();

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(NicknameDuplicateException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATE.getMessage());
    }
}
