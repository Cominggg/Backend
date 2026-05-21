package com.Coming.Backend.concert.entity;

public enum ConcertStatus {
    UPCOMING, ONGOING, ENDED, CANCELLED;

    public String toDisplayName() {
        return switch (this) {
            case UPCOMING -> "공연예정";
            case ONGOING -> "공연중";
            case ENDED -> "공연완료";
            case CANCELLED -> "공연취소";
        };
    }
}
