package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.ArtistAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistAliasRepository extends JpaRepository<ArtistAlias, Long> {

    List<ArtistAlias> findByArtistId(Long artistId);

    Optional<ArtistAlias> findFirstByArtistIdAndLocaleOrderByIdAsc(Long artistId, String locale);

    List<ArtistAlias> findByArtistIdInAndLocale(List<Long> artistIds, String locale);
}
