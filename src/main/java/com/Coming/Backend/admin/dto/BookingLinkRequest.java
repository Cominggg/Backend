package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingLinkRequest(
        @NotBlank
        String name,

        @NotBlank
        String url
) {
}
