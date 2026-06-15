package com.Coming.Backend.release.repository;

import com.Coming.Backend.release.entity.ReleaseGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReleaseGroupRepository extends JpaRepository<ReleaseGroup, Long> {

    List<ReleaseGroup> findByArtistId(Long artistId);

    Optional<ReleaseGroup> findByMbid(String mbid);

    Page<ReleaseGroup> findByArtistId(Long artistId, Pageable pageable);

    Page<ReleaseGroup> findByArtistIdAndTypeIn(Long artistId, List<String> types, Pageable pageable);

    Page<ReleaseGroup> findByType(String type, Pageable pageable);

    Page<ReleaseGroup> findByArtistIdAndType(Long artistId, String type, Pageable pageable);

    @Query("SELECT r FROM ReleaseGroup r WHERE r.type NOT IN :standardTypes")
    Page<ReleaseGroup> findByTypeNotIn(@Param("standardTypes") List<String> standardTypes, Pageable pageable);

    @Query("SELECT r FROM ReleaseGroup r WHERE r.artistId = :artistId AND r.type NOT IN :standardTypes")
    Page<ReleaseGroup> findByArtistIdAndTypeNotIn(@Param("artistId") Long artistId, @Param("standardTypes") List<String> standardTypes, Pageable pageable);

    Page<ReleaseGroup> findByArtistIdIn(List<Long> artistIds, Pageable pageable);

    Page<ReleaseGroup> findByArtistIdInAndType(List<Long> artistIds, String type, Pageable pageable);

    @Query("SELECT r FROM ReleaseGroup r WHERE r.artistId IN :artistIds AND r.type NOT IN :standardTypes")
    Page<ReleaseGroup> findByArtistIdInAndTypeNotIn(@Param("artistIds") List<Long> artistIds, @Param("standardTypes") List<String> standardTypes, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT r FROM ReleaseGroup r
            WHERE (
                LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR r.id IN (
                    SELECT t.releaseGroupId FROM Track t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                OR r.artistId IN (
                    SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                OR r.artistId IN (
                    SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :q, '%'))
                )
            )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT r) FROM ReleaseGroup r
            WHERE (
                LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR r.id IN (
                    SELECT t.releaseGroupId FROM Track t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                OR r.artistId IN (
                    SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                OR r.artistId IN (
                    SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE LOWER(CONCAT('%', :q, '%'))
                )
            )
            """)
    Page<ReleaseGroup> searchReleases(@Param("q") String q, Pageable pageable);
}
