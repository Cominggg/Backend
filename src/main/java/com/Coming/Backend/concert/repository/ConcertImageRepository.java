package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcertImageRepository extends JpaRepository<ConcertImage, Long> {

    List<ConcertImage> findByConcertIdOrderByPosition(Long concertId);

    @Modifying
    @Query("DELETE FROM ConcertImage ci WHERE ci.concertId = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
