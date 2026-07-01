package com.Coming.Backend.admin.dto;

import java.util.List;

public record AdminArtistUpdateRequest(
        String name,
        String sortName,
        AliasesRequest aliases
) {
    public record AliasesRequest(List<String> ja, List<String> en, List<String> ko) {}
}
