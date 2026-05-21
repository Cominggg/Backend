package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record AdminArtistUpdateRequest(
        String name,
        String sortName,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate debutDate
) {
}
