package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserStatusValidatorTest {

    @InjectMocks
    private UserStatusValidator userStatusValidator;

    @Mock
    private UserRepository userRepository;

    private static final Long USER_ID = 1L;

    private User buildUser(UserStatus status) {
        return User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(status)
                .provider("google")
                .providerId("google-123")
                .build();
    }

    @Test
    void should_pass_when_user_is_active() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(UserStatus.ACTIVE)));

        // when & then
        assertThatCode(() -> userStatusValidator.validateWritable(USER_ID)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_UserSuspendedException_when_user_is_suspended() {
        // given
        User user = buildUser(UserStatus.ACTIVE);
        user.suspend(LocalDateTime.now().plusDays(7), ReportReason.SPAM, null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userStatusValidator.validateWritable(USER_ID))
                .isInstanceOf(UserSuspendedException.class);
    }

    @Test
    void should_pass_when_suspension_period_has_passed() {
        // given
        User user = buildUser(UserStatus.ACTIVE);
        user.suspend(LocalDateTime.now().minusMinutes(1), ReportReason.SPAM, null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatCode(() -> userStatusValidator.validateWritable(USER_ID)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_UserNotFoundException_when_user_has_withdrawn() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(UserStatus.INACTIVE)));

        // when & then
        assertThatThrownBy(() -> userStatusValidator.validateWritable(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void should_throw_UserNotFoundException_when_user_does_not_exist() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userStatusValidator.validateWritable(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }
}
