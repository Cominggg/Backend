package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertBookingLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcertBookingLinkRepository extends JpaRepository<ConcertBookingLink, Long> {

    List<ConcertBookingLink> findByConcertId(Long concertId);

    List<ConcertBookingLink> findByConcertIdIn(List<Long> concertIds);

    @Modifying
    @Query("DELETE FROM ConcertBookingLink cbl WHERE cbl.concertId = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
