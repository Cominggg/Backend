package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    boolean existsByMbid(String mbid);

    // ── name 검색 (alias 포함) ──────────────────────────────────────────────

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

    @Query(
            value = """
                    SELECT DISTINCT a FROM Artist a
                    WHERE a.isComing = :isComing
                      AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           OR a.id IN (
                               SELECT al.artistId FROM ArtistAlias al
                               WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           ))
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT a) FROM Artist a
                    WHERE a.isComing = :isComing
                      AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           OR a.id IN (
                               SELECT al.artistId FROM ArtistAlias al
                               WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           ))
                    """
    )
    Page<Artist> findByIsComingAndNameOrAliasContainingIgnoreCase(
            @Param("isComing") boolean isComing,
            @Param("name") String name,
            Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT a FROM Artist a
                    WHERE a.id IN :ids
                      AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           OR a.id IN (
                               SELECT al.artistId FROM ArtistAlias al
                               WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           ))
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT a) FROM Artist a
                    WHERE a.id IN :ids
                      AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           OR a.id IN (
                               SELECT al.artistId FROM ArtistAlias al
                               WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           ))
                    """
    )
    Page<Artist> findByIdInAndNameOrAliasContainingIgnoreCase(
            @Param("ids") List<Long> ids,
            @Param("name") String name,
            Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT a FROM Artist a
                    WHERE a.isComing = :isComing
                      AND a.id IN :ids
                      AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           OR a.id IN (
                               SELECT al.artistId FROM ArtistAlias al
                               WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           ))
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT a) FROM Artist a
                    WHERE a.isComing = :isComing
                      AND a.id IN :ids
                      AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           OR a.id IN (
                               SELECT al.artistId FROM ArtistAlias al
                               WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :name, '%'))
                           ))
                    """
    )
    Page<Artist> findByIsComingAndIdInAndNameOrAliasContainingIgnoreCase(
            @Param("isComing") boolean isComing,
            @Param("ids") List<Long> ids,
            @Param("name") String name,
            Pageable pageable);

    // ── name 없음 (파생 쿼리) ────────────────────────────────────────────────

    Page<Artist> findByIsComing(boolean isComing, Pageable pageable);

    Page<Artist> findAllByIdIn(List<Long> ids, Pageable pageable);

    Page<Artist> findByIsComingAndIdIn(boolean isComing, List<Long> ids, Pageable pageable);
}
