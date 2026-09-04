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

    /**
     * artistId·followedArtistIds·type(exactType 또는 기타 NOT IN standardTypes)·q 필터를 조합해 릴리즈를 조회한다.
     * 각 필터는 대응 파라미터가 null(또는 false)이면 조건 없이 통과된다.
     */
    @Query(value = """
            SELECT r FROM ReleaseGroup r
            WHERE (:artistId IS NULL OR r.artistId = :artistId)
            AND (:followedArtistIds IS NULL OR r.artistId IN :followedArtistIds)
            AND (:exactType IS NULL OR r.type = :exactType)
            AND (:otherType = false OR r.type NOT IN :standardTypes)
            AND (:q IS NULL OR
                LOWER(r.title) LIKE :q
                OR r.id IN (
                    SELECT t.releaseGroupId FROM Track t WHERE LOWER(t.title) LIKE :q
                )
                OR r.artistId IN (
                    SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q OR LOWER(a.sortName) LIKE :q
                )
                OR r.artistId IN (
                    SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                )
            )
            """,
            countQuery = """
            SELECT COUNT(r) FROM ReleaseGroup r
            WHERE (:artistId IS NULL OR r.artistId = :artistId)
            AND (:followedArtistIds IS NULL OR r.artistId IN :followedArtistIds)
            AND (:exactType IS NULL OR r.type = :exactType)
            AND (:otherType = false OR r.type NOT IN :standardTypes)
            AND (:q IS NULL OR
                LOWER(r.title) LIKE :q
                OR r.id IN (
                    SELECT t.releaseGroupId FROM Track t WHERE LOWER(t.title) LIKE :q
                )
                OR r.artistId IN (
                    SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q OR LOWER(a.sortName) LIKE :q
                )
                OR r.artistId IN (
                    SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                )
            )
            """)
    Page<ReleaseGroup> searchReleases(@Param("artistId") Long artistId,
                                       @Param("followedArtistIds") List<Long> followedArtistIds,
                                       @Param("exactType") String exactType,
                                       @Param("otherType") boolean otherType,
                                       @Param("standardTypes") List<String> standardTypes,
                                       @Param("q") String q,
                                       Pageable pageable);
}
