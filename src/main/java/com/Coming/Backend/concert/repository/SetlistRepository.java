package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.Setlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    List<Setlist> findByConcertId(Long concertId);

    List<Setlist> findByConcertIdOrderByCollectedAtDesc(Long concertId);

    Optional<Setlist> findBySetlistFmId(String setlistFmId);
}
