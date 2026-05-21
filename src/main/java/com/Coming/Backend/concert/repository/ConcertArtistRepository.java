package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConcertArtistRepository extends JpaRepository<ConcertArtist, Long> {

    List<ConcertArtist> findByConcertId(Long concertId);

    List<ConcertArtist> findByArtistId(Long artistId);

    Optional<ConcertArtist> findFirstByConcertIdAndConfidence(Long concertId, String confidence);

    List<ConcertArtist> findByConcertIdInAndConfidence(List<Long> concertIds, String confidence);
}
