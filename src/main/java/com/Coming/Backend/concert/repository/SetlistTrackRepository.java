package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.SetlistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SetlistTrackRepository extends JpaRepository<SetlistTrack, Long> {

    List<SetlistTrack> findBySetlistIdOrderByPosition(Long setlistId);

    @Modifying
    @Query("DELETE FROM SetlistTrack st WHERE st.setlistId IN :setlistIds")
    void deleteBySetlistIdIn(@Param("setlistIds") List<Long> setlistIds);
}
