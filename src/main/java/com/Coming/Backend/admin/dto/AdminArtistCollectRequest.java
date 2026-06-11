package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminArtistCollectRequest(
        @NotBlank String mbid
) {
}
