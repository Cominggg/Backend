package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Optional<Concert> findByKopisId(String kopisId);

    boolean existsByKopisId(String kopisId);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.startDate >= :since")
    Page<Concert> findAllByArtistId(@Param("artistId") Long artistId, @Param("since") LocalDate since, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.status IN :statuses AND c.startDate >= :since")
    Page<Concert> findAllByArtistIdAndStatusIn(@Param("artistId") Long artistId, @Param("statuses") List<ConcertStatus> statuses, @Param("since") LocalDate since, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE (:status IS NULL OR c.status = :status)")
    Page<Concert> findConcerts(@Param("status") ConcertStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Concert c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
