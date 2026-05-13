package com.Coming.Backend.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    List<Setlist> findByConcertId(Long concertId);

    Optional<Setlist> findBySetlistFmId(String setlistFmId);
}
