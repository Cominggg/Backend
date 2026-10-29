package com.Coming.Backend.auth.entity;

import com.Coming.Backend.common.entity.BaseTimeEntity;
import com.Coming.Backend.report.entity.ReportReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"user\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseTimeEntity {

    private static final String WITHDRAWN_PROVIDER_ID_PREFIX = "withdrawn:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "agreed_marketing")
    private Boolean agreedMarketing;

    @Column(name = "apple_refresh_token", length = 512)
    private String appleRefreshToken;

    /** 정지 만료 시각. 정지(SUSPENDED) 상태에서 null이면 영구 정지. */
    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_reason", length = 20)
    private ReportReason suspensionReason;

    @Column(name = "suspension_detail", columnDefinition = "TEXT")
    private String suspensionDetail;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public void completeRegistration(String nickname, int birthYear, boolean agreedMarketing) {
        this.nickname = nickname;
        this.birthYear = birthYear;
        this.agreedMarketing = agreedMarketing;
        this.role = UserRole.USER;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateMarketing(boolean agreedMarketing) {
        this.agreedMarketing = agreedMarketing;
    }

    /**
     * 탈퇴 처리한다. 개인정보를 지우고 provider_id를 바꿔 같은 소셜 계정으로 다시 로그인하면 새 회원으로 가입되게 한다.
     */
    public void withdraw() {
        this.status = UserStatus.INACTIVE;
        this.providerId = WITHDRAWN_PROVIDER_ID_PREFIX + id;
        this.email = null;
        this.nickname = null;
        this.birthYear = null;
        this.agreedMarketing = null;
        this.withdrawnAt = LocalDateTime.now();
        clearSuspension();
    }

    /**
     * 회원을 정지한다.
     *
     * @param until null이면 영구 정지
     */
    public void suspend(LocalDateTime until, ReportReason reason, String detail) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
        this.suspensionReason = reason;
        this.suspensionDetail = detail;
    }

    public void unsuspend() {
        this.status = UserStatus.ACTIVE;
        clearSuspension();
    }

    /**
     * 기간 정지가 만료됐는지 확인한다. 영구 정지이거나 정지 상태가 아니면 false.
     */
    public boolean isSuspensionExpired() {
        return status == UserStatus.SUSPENDED
                && suspendedUntil != null
                && !LocalDateTime.now().isBefore(suspendedUntil);
    }

    private void clearSuspension() {
        this.suspendedUntil = null;
        this.suspensionReason = null;
        this.suspensionDetail = null;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateAppleRefreshToken(String appleRefreshToken) {
        this.appleRefreshToken = appleRefreshToken;
    }

    public boolean hasNoEmail() {
        return email == null || email.isBlank();
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.role = UserRole.PENDING;
        this.nickname = null;
        this.birthYear = null;
        this.agreedMarketing = null;
    }
}
