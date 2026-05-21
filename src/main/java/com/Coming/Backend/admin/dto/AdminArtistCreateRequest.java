package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record AdminArtistCreateRequest(
        @NotBlank
        @Pattern(
                regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                message = "UUID 형식이어야 합니다."
        )
        String mbid,

        @NotBlank
        String name,

        String sortName,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate debutDate
) {
}
