package com.Coming.Backend.auth.entity;

import com.Coming.Backend.common.entity.BaseTimeEntity;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "agreed_terms")
    private Boolean agreedTerms;

    @Column(name = "agreed_privacy")
    private Boolean agreedPrivacy;

    @Column(name = "agreed_marketing")
    private Boolean agreedMarketing;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    public void completeRegistration(String nickname, int birthYear,
            boolean agreedTerms, boolean agreedPrivacy, boolean agreedMarketing) {
        this.nickname = nickname;
        this.birthYear = birthYear;
        this.agreedTerms = agreedTerms;
        this.agreedPrivacy = agreedPrivacy;
        this.agreedMarketing = agreedMarketing;
        this.agreedAt = LocalDateTime.now();
        this.role = UserRole.USER;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void withdraw() {
        this.status = UserStatus.INACTIVE;
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.role = UserRole.PENDING;
        this.nickname = null;
        this.birthYear = null;
        this.agreedTerms = null;
        this.agreedPrivacy = null;
        this.agreedMarketing = null;
        this.agreedAt = null;
    }
}
