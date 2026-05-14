package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    boolean existsByMbid(String mbid);
}
