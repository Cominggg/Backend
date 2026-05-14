package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertArtistRepository extends JpaRepository<ConcertArtist, Long> {

    List<ConcertArtist> findByConcertId(Long concertId);

    List<ConcertArtist> findByArtistId(Long artistId);
}
