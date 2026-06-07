package com.Coming.Backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminConcertArtistAssignRequest(
        @NotNull Long artistId
) {
}
