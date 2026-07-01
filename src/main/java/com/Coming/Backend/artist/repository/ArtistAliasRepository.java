package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.ArtistAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ArtistAliasRepository extends JpaRepository<ArtistAlias, Long> {

    List<ArtistAlias> findByArtistId(Long artistId);

    void deleteByArtistIdAndLocaleIn(Long artistId, Collection<String> locales);
}
