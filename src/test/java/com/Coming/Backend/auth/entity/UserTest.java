package com.Coming.Backend.auth.entity;

import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final Long USER_ID = 1L;

    private User buildActiveUser() {
        return User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .provider("kakao")
                .providerId("12345")
                .nickname("테스터")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .birthYear(2000)
                .agreedMarketing(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // withdraw
    // -------------------------------------------------------------------------

    @Test
    void should_clear_personal_info_and_replace_provider_id_when_withdraw() {
        // given
        User user = buildActiveUser();

        // when
        user.withdraw();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.getProviderId()).isEqualTo("withdrawn:" + USER_ID);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getBirthYear()).isNull();
        assertThat(user.getAgreedMarketing()).isNull();
        assertThat(user.getWithdrawnAt()).isNotNull();
    }

    @Test
    void should_clear_suspension_info_when_suspended_user_withdraws() {
        // given
        User user = buildActiveUser();
        user.suspend(LocalDateTime.now().plusDays(7), ReportReason.SPAM, "도배");

        // when
        user.withdraw();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.getSuspensionReason()).isNull();
        assertThat(user.getSuspensionDetail()).isNull();
    }

    // -------------------------------------------------------------------------
    // suspend / unsuspend
    // -------------------------------------------------------------------------

    @Test
    void should_set_suspension_info_when_suspend() {
        // given
        User user = buildActiveUser();
        LocalDateTime until = LocalDateTime.now().plusDays(7);

        // when
        user.suspend(until, ReportReason.ABUSE, "욕설");

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getSuspendedUntil()).isEqualTo(until);
        assertThat(user.getSuspensionReason()).isEqualTo(ReportReason.ABUSE);
        assertThat(user.getSuspensionDetail()).isEqualTo("욕설");
    }

    @Test
    void should_restore_active_and_clear_suspension_info_when_unsuspend() {
        // given
        User user = buildActiveUser();
        user.suspend(null, ReportReason.ILLEGAL, "불법 행위");

        // when
        user.unsuspend();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.getSuspensionReason()).isNull();
        assertThat(user.getSuspensionDetail()).isNull();
    }

    // -------------------------------------------------------------------------
    // isSuspensionExpired
    // -------------------------------------------------------------------------

    @Test
    void should_return_true_when_suspension_period_has_passed() {
        // given
        User user = buildActiveUser();
        user.suspend(LocalDateTime.now().minusSeconds(1), ReportReason.SPAM, null);

        // when & then
        assertThat(user.isSuspensionExpired()).isTrue();
    }

    @Test
    void should_return_false_when_suspension_period_remains() {
        // given
        User user = buildActiveUser();
        user.suspend(LocalDateTime.now().plusDays(1), ReportReason.SPAM, null);

        // when & then
        assertThat(user.isSuspensionExpired()).isFalse();
    }

    @Test
    void should_return_false_when_suspension_is_permanent() {
        // given
        User user = buildActiveUser();
        user.suspend(null, ReportReason.SPAM, null);

        // when & then
        assertThat(user.isSuspensionExpired()).isFalse();
    }

    @Test
    void should_return_false_when_user_is_not_suspended() {
        // given
        User user = buildActiveUser();

        // when & then
        assertThat(user.isSuspensionExpired()).isFalse();
    }
}
