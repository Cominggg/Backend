package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.SetlistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SetlistTrackRepository extends JpaRepository<SetlistTrack, Long> {

    List<SetlistTrack> findBySetlistIdOrderByPosition(Long setlistId);

    void deleteBySetlistIdIn(List<Long> setlistIds);
}
