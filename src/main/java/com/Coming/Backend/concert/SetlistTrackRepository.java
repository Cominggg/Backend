package com.Coming.Backend.concert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SetlistTrackRepository extends JpaRepository<SetlistTrack, Long> {

    List<SetlistTrack> findBySetlistIdOrderByPosition(Long setlistId);
}
