package com.Coming.Backend.admin.dto;

public record AdminArtistUpdateRequest(
        String name,
        String sortName,
        AliasesRequest aliases
) {
    public record AliasesRequest(String ja, String en, String ko) {}
}
