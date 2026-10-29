package com.Coming.Backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.auth.dto.SuspensionCommand;
import com.Coming.Backend.auth.entity.SuspensionDuration;
import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import com.Coming.Backend.auth.event.SuspensionNoticeEvent;
import com.Coming.Backend.auth.exception.UserNotFoundException;
import com.Coming.Backend.auth.repository.TokenRepository;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SuspensionServiceTest {

    @InjectMocks
    private SuspensionService suspensionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";

    private User buildUser(UserStatus status, String email) {
        return User.builder()
                .id(USER_ID)
                .email(email)
                .nickname("테스터")
                .role(UserRole.USER)
                .status(status)
                .provider("google")
                .providerId("google-123")
                .build();
    }

    // -------------------------------------------------------------------------
    // suspend
    // -------------------------------------------------------------------------

    @Test
    void should_suspend_user_and_delete_all_sessions_and_publish_notice_when_suspend() {
        // given
        User user = buildUser(UserStatus.ACTIVE, EMAIL);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        SuspensionCommand command = new SuspensionCommand(SuspensionDuration.DAYS_7, ReportReason.SPAM, "도배");

        // when
        suspensionService.suspend(USER_ID, command);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getSuspendedUntil()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(user.getSuspendedUntil().getNano()).isZero();
        assertThat(user.getSuspensionReason()).isEqualTo(ReportReason.SPAM);
        assertThat(user.getSuspensionDetail()).isEqualTo("도배");
        verify(tokenRepository).deleteAllSessions(USER_ID);
        ArgumentCaptor<SuspensionNoticeEvent> captor = ArgumentCaptor.forClass(SuspensionNoticeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo(EMAIL);
        assertThat(captor.getValue().suspendedUntil()).isEqualTo(user.getSuspendedUntil());
        assertThat(captor.getValue().reason()).isEqualTo(ReportReason.SPAM);
        assertThat(captor.getValue().detail()).isEqualTo("도배");
    }

    @Test
    void should_set_null_until_when_permanent_suspension() {
        // given
        User user = buildUser(UserStatus.ACTIVE, EMAIL);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        suspensionService.suspend(USER_ID,
                new SuspensionCommand(SuspensionDuration.PERMANENT, ReportReason.ILLEGAL, null));

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getSuspendedUntil()).isNull();
    }

    @Test
    void should_not_publish_notice_when_user_has_no_email() {
        // given
        User user = buildUser(UserStatus.ACTIVE, null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        suspensionService.suspend(USER_ID,
                new SuspensionCommand(SuspensionDuration.DAYS_30, ReportReason.ABUSE, null));

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(eventPublisher, never()).publishEvent(any(SuspensionNoticeEvent.class));
    }

    @Test
    void should_throw_UserNotFoundException_when_suspending_withdrawn_user() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(UserStatus.INACTIVE, null)));

        // when & then
        assertThatThrownBy(() -> suspensionService.suspend(USER_ID,
                new SuspensionCommand(SuspensionDuration.DAYS_7, ReportReason.SPAM, null)))
                .isInstanceOf(UserNotFoundException.class);
        verify(tokenRepository, never()).deleteAllSessions(any());
    }

    @Test
    void should_throw_UserNotFoundException_when_suspending_nonexistent_user() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> suspensionService.suspend(USER_ID,
                new SuspensionCommand(SuspensionDuration.DAYS_7, ReportReason.SPAM, null)))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // unsuspend
    // -------------------------------------------------------------------------

    @Test
    void should_restore_active_when_unsuspend_suspended_user() {
        // given
        User user = buildUser(UserStatus.ACTIVE, EMAIL);
        user.suspend(null, ReportReason.SPAM, null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        suspensionService.unsuspend(USER_ID);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getSuspensionReason()).isNull();
    }

    @Test
    void should_keep_active_when_unsuspend_active_user() {
        // given
        User user = buildUser(UserStatus.ACTIVE, EMAIL);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        suspensionService.unsuspend(USER_ID);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_throw_UserNotFoundException_when_unsuspending_withdrawn_user() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(buildUser(UserStatus.INACTIVE, null)));

        // when & then
        assertThatThrownBy(() -> suspensionService.unsuspend(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }
}
