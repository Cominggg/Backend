package com.Coming.Backend.artist;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    boolean existsByMbid(String mbid);
}
