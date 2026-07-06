package com.Coming.Backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 20)
        String nickname,

        @NotNull
        Integer birthYear,

        @NotNull
        Boolean agreedTerms,

        @NotNull
        Boolean agreedPrivacy,

        Boolean agreedMarketing
) {
}
