package com.Coming.Backend.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Optional<Concert> findByKopisId(String kopisId);

    boolean existsByKopisId(String kopisId);
}
