package com.Coming.Backend.auth.dto;

public record RegisterRequest(
        String nickname,
        Integer birthYear,
        Boolean agreedTerms,
        Boolean agreedPrivacy,
        Boolean agreedMarketing
) {
}
