package com.Coming.Backend.artist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistAliasRepository extends JpaRepository<ArtistAlias, Long> {

    List<ArtistAlias> findByArtistId(Long artistId);
}
