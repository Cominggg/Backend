package com.Coming.Backend.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertArtistRepository extends JpaRepository<ConcertArtist, Long> {

    List<ConcertArtist> findByConcertId(Long concertId);

    List<ConcertArtist> findByArtistId(Long artistId);
}
