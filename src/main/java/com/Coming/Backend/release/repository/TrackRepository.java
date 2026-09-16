package com.Coming.Backend.release.repository;

import com.Coming.Backend.release.entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    List<Track> findByReleaseGroupIdOrderByPosition(Long releaseGroupId);

    List<Track> findByReleaseGroupIdInOrderByPosition(Collection<Long> releaseGroupIds);

    @Query(value = "SELECT t FROM Track t WHERE LOWER(t.title) LIKE :q ESCAPE '\\' ORDER BY t.id ASC",
            countQuery = "SELECT COUNT(t) FROM Track t WHERE LOWER(t.title) LIKE :q ESCAPE '\\'")
    Page<Track> searchByTitleForMention(@Param("q") String q, Pageable pageable);
}
