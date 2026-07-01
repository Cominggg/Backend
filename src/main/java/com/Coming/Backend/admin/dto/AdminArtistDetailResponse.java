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
    public record AliasesDto(List<String> ja, List<String> en, List<String> ko) {}

    public static AdminArtistDetailResponse of(Artist artist, List<ArtistAlias> aliases) {
        Map<String, List<String>> aliasMap = aliases.stream()
                .filter(a -> a.getLocale() != null)
                .collect(Collectors.groupingBy(
                        ArtistAlias::getLocale,
                        Collectors.mapping(ArtistAlias::getName, Collectors.toList())
                ));
        return new AdminArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                new AliasesDto(
                        aliasMap.getOrDefault("ja", List.of()),
                        aliasMap.getOrDefault("en", List.of()),
                        aliasMap.getOrDefault("ko", List.of())
                )
        );
    }
}
