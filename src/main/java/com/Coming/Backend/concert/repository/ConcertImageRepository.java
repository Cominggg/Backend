package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertImageRepository extends JpaRepository<ConcertImage, Long> {

    List<ConcertImage> findByConcertIdOrderByPosition(Long concertId);
}
