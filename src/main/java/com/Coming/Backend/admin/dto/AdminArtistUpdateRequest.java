package com.Coming.Backend.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AdminArtistUpdateRequest(
        String name,
        String sortName,
        String imageUrl,
        AliasesRequest aliases,
        @Valid List<LinkRequest> links
) {
    public record AliasesRequest(List<String> ja, List<String> en, List<String> ko) {}

    public record LinkRequest(
            @NotBlank
            String type,

            @NotBlank
            String url
    ) {}
}
