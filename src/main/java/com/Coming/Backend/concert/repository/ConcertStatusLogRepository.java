package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertStatusLogRepository extends JpaRepository<ConcertStatusLog, Long> {

    void deleteByConcertId(Long concertId);
}
