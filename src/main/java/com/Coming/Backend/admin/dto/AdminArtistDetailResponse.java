package com.Coming.Backend.admin.dto;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record AdminArtistDetailResponse(
        Long id,
        String name,
        AliasesDto aliases
) {
    public record AliasesDto(String ja, String en, String ko) {}

    public static AdminArtistDetailResponse of(Artist artist, List<ArtistAlias> aliases) {
        Map<String, String> aliasMap = aliases.stream()
                .filter(a -> a.getLocale() != null)
                .collect(Collectors.toMap(ArtistAlias::getLocale, ArtistAlias::getName));
        return new AdminArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                new AliasesDto(
                        aliasMap.get("ja"),
                        aliasMap.get("en"),
                        aliasMap.get("ko")
                )
        );
    }
}
