package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertArtistCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcertArtistCandidateRepository extends JpaRepository<ConcertArtistCandidate, Long> {

    List<ConcertArtistCandidate> findByConcertId(Long concertId);

    boolean existsByConcertIdAndArtistId(Long concertId, Long artistId);

    Optional<ConcertArtistCandidate> findByConcertIdAndArtistId(Long concertId, Long artistId);

    List<ConcertArtistCandidate> findByConcertIdIn(List<Long> concertIds);

    @Modifying
    @Query("DELETE FROM ConcertArtistCandidate c WHERE c.concertId = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
