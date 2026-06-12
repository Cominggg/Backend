package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcertArtistRepository extends JpaRepository<ConcertArtist, Long> {

    List<ConcertArtist> findByConcertId(Long concertId);

    List<ConcertArtist> findByArtistId(Long artistId);

    Optional<ConcertArtist> findFirstByConcertIdOrderByIdAsc(Long concertId);

    List<ConcertArtist> findByConcertIdIn(List<Long> concertIds);

    boolean existsByConcertIdAndArtistId(Long concertId, Long artistId);

    Optional<ConcertArtist> findByConcertIdAndArtistId(Long concertId, Long artistId);

    @Modifying
    @Query("DELETE FROM ConcertArtist ca WHERE ca.concertId = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
