package com.Coming.Backend.admin.dto;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.entity.ArtistUrl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record AdminArtistDetailResponse(
        Long id,
        String name,
        String imageUrl,
        AliasesDto aliases,
        List<LinkDto> links
) {
    public record AliasesDto(List<String> ja, List<String> en, List<String> ko) {}

    public record LinkDto(String type, String url) {}

    public static AdminArtistDetailResponse of(Artist artist, List<ArtistAlias> aliases, List<ArtistUrl> links) {
        Map<String, List<String>> aliasMap = aliases.stream()
                .filter(a -> a.getLocale() != null)
                .collect(Collectors.groupingBy(
                        ArtistAlias::getLocale,
                        Collectors.mapping(ArtistAlias::getName, Collectors.toList())
                ));
        return new AdminArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                artist.getImageUrl(),
                new AliasesDto(
                        aliasMap.getOrDefault("ja", List.of()),
                        aliasMap.getOrDefault("en", List.of()),
                        aliasMap.getOrDefault("ko", List.of())
                ),
                links.stream().map(link -> new LinkDto(link.getType(), link.getUrl())).toList()
        );
    }
}
