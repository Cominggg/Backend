package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertArtistCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcertArtistCandidateRepository extends JpaRepository<ConcertArtistCandidate, Long> {

    List<ConcertArtistCandidate> findByConcertId(Long concertId);

    @Modifying
    @Query("DELETE FROM ConcertArtistCandidate c WHERE c.concertId = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
