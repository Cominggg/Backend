package com.Coming.Backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppRefreshRequest(
        @NotBlank
        String refreshToken
) {
}
