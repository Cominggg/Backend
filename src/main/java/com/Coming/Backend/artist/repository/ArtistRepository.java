package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    boolean existsByMbid(String mbid);

    @Query(
            value = """
                    SELECT DISTINCT a FROM Artist a
                    WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                       OR a.id IN (
                           SELECT al.artistId FROM ArtistAlias al
                           WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                       )
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT a) FROM Artist a
                    WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                       OR a.id IN (
                           SELECT al.artistId FROM ArtistAlias al
                           WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                       )
                    """
    )
    Page<Artist> findByNameOrAliasContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}
