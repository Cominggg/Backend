package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.ArtistUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistUrlRepository extends JpaRepository<ArtistUrl, Long> {

    List<ArtistUrl> findByArtistId(Long artistId);
}
