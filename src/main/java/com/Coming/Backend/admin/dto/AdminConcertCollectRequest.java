package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminConcertCollectRequest(
        @NotBlank String kopisId
) {
}
