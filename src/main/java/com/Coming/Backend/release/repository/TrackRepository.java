package com.Coming.Backend.release.repository;

import com.Coming.Backend.release.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    List<Track> findByReleaseGroupIdOrderByPosition(Long releaseGroupId);
}
