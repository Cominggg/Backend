package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.ConcertBookingLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertBookingLinkRepository extends JpaRepository<ConcertBookingLink, Long> {

    List<ConcertBookingLink> findByConcertId(Long concertId);
}
