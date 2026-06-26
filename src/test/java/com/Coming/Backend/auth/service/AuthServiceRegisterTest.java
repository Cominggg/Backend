package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.auth.dto.NicknameCheckResponse;
import com.Coming.Backend.auth.dto.RegisterRequest;
import com.Coming.Backend.auth.dto.TokenResponse;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.BirthYearRequiredException;
import com.Coming.Backend.auth.exception.NicknameDuplicateException;
import com.Coming.Backend.auth.exception.NicknameRequiredException;
import com.Coming.Backend.auth.exception.NicknameTooLongException;
import com.Coming.Backend.auth.exception.TermsNotAgreedException;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.jwt.JwtProvider;
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
class AuthServiceRegisterTest {

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
    private static final String NEW_ACCESS_TOKEN = "new-access-token";

    private User buildPendingUser() {
        return User.builder()
                .id(USER_ID)
                .nickname(null)
                .role(UserRole.PENDING)
                .status(UserStatus.ACTIVE)
                .provider("google")
                .providerId("google-123")
                .build();
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void should_return_token_and_change_role_to_user_when_registration_is_valid() {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        User user = buildPendingUser();
        given(userRepository.existsByNickname("IU")).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(USER_ID, UserRole.USER.name())).willReturn(NEW_ACCESS_TOKEN);

        // when
        TokenResponse response = authService.register(USER_ID, request);

        // then
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getNickname()).isEqualTo("IU");
    }

    @Test
    void should_throw_terms_not_agreed_exception_when_agreed_terms_is_false() {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, false, true, false);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(TermsNotAgreedException.class)
                .hasMessage(ErrorCode.TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void should_throw_terms_not_agreed_exception_when_agreed_privacy_is_false() {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, false, false);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(TermsNotAgreedException.class)
                .hasMessage(ErrorCode.TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void should_throw_nickname_required_exception_when_nickname_is_null() {
        // given
        RegisterRequest request = new RegisterRequest(null, 1993, true, true, false);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(NicknameRequiredException.class)
                .hasMessage(ErrorCode.NICKNAME_REQUIRED.getMessage());
    }

    @Test
    void should_throw_nickname_required_exception_when_nickname_is_blank() {
        // given
        RegisterRequest request = new RegisterRequest("   ", 1993, true, true, false);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(NicknameRequiredException.class)
                .hasMessage(ErrorCode.NICKNAME_REQUIRED.getMessage());
    }

    @Test
    void should_throw_nickname_too_long_exception_when_nickname_exceeds_20_chars() {
        // given
        String tooLongNickname = "a".repeat(21);
        RegisterRequest request = new RegisterRequest(tooLongNickname, 1993, true, true, false);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(NicknameTooLongException.class)
                .hasMessage(ErrorCode.NICKNAME_TOO_LONG.getMessage());
    }

    @Test
    void should_throw_nickname_duplicate_exception_when_nickname_already_exists() {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(userRepository.existsByNickname("IU")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(NicknameDuplicateException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATE.getMessage());
    }

    @Test
    void should_throw_birth_year_required_exception_when_birth_year_is_null() {
        // given
        RegisterRequest request = new RegisterRequest("IU", null, true, true, false);
        given(userRepository.existsByNickname("IU")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(BirthYearRequiredException.class)
                .hasMessage(ErrorCode.BIRTH_YEAR_REQUIRED.getMessage());
    }

    @Test
    void should_throw_user_not_found_exception_when_user_does_not_exist() {
        // given
        RegisterRequest request = new RegisterRequest("IU", 1993, true, true, false);
        given(userRepository.existsByNickname("IU")).willReturn(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.register(USER_ID, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // -------------------------------------------------------------------------
    // checkNickname
    // -------------------------------------------------------------------------

    @Test
    void should_return_available_true_when_nickname_does_not_exist() {
        // given
        given(userRepository.existsByNickname("IU")).willReturn(false);

        // when
        NicknameCheckResponse response = authService.checkNickname("IU");

        // then
        assertThat(response.available()).isTrue();
    }

    @Test
    void should_return_available_false_when_nickname_already_exists() {
        // given
        given(userRepository.existsByNickname("IU")).willReturn(true);

        // when
        NicknameCheckResponse response = authService.checkNickname("IU");

        // then
        assertThat(response.available()).isFalse();
    }
}
