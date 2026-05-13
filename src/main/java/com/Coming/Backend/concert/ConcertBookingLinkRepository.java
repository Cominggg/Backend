package com.Coming.Backend.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertBookingLinkRepository extends JpaRepository<ConcertBookingLink, Long> {

    List<ConcertBookingLink> findByConcertId(Long concertId);
}
