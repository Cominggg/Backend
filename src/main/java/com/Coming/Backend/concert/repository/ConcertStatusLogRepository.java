package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConcertStatusLogRepository extends JpaRepository<ConcertStatusLog, Long> {

    @Modifying
    @Query("DELETE FROM ConcertStatusLog csl WHERE csl.concertId = :concertId")
    void deleteByConcertId(@Param("concertId") Long concertId);
}
