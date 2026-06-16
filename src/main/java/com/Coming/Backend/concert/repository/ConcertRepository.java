package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Optional<Concert> findByKopisId(String kopisId);

    boolean existsByKopisId(String kopisId);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.startDate >= :since AND c.status NOT IN :hidden")
    Page<Concert> findAllByArtistId(@Param("artistId") Long artistId, @Param("since") LocalDate since, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.status IN :statuses AND c.startDate >= :since")
    Page<Concert> findAllByArtistIdAndStatusIn(@Param("artistId") Long artistId, @Param("statuses") List<ConcertStatus> statuses, @Param("since") LocalDate since, Pageable pageable);

    Page<Concert> findByStatus(ConcertStatus status, Pageable pageable);

    Page<Concert> findByStatusNotIn(Collection<ConcertStatus> hidden, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :artistIds) AND c.status NOT IN :hidden ORDER BY c.startDate DESC")
    List<Concert> findAllByArtistIdIn(@Param("artistIds") List<Long> artistIds, @Param("hidden") Collection<ConcertStatus> hidden);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :artistIds) AND c.status = :status ORDER BY c.startDate DESC")
    List<Concert> findAllByArtistIdInAndStatus(@Param("artistIds") List<Long> artistIds, @Param("status") ConcertStatus status);

    @Modifying
    @Query("UPDATE Concert c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT c FROM Concert c WHERE c.startDate <= :lastDay AND c.endDate >= :firstDay AND c.status NOT IN :hidden ORDER BY c.startDate ASC")
    List<Concert> findByDateRange(@Param("firstDay") LocalDate firstDay, @Param("lastDay") LocalDate lastDay, @Param("hidden") Collection<ConcertStatus> hidden);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :userId) AND c.status NOT IN :hidden")
    Page<Concert> findByUserCalendar(@Param("userId") Long userId, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    List<Concert> findTop10ByStatusNotInOrderByViewCountDesc(Collection<ConcertStatus> hidden);

    @Query("SELECT COUNT(c) FROM Concert c WHERE YEAR(c.startDate) = :year AND MONTH(c.startDate) = :month")
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(c) > 0 FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.status IN :statuses")
    boolean existsActiveByArtistId(@Param("artistId") Long artistId, @Param("statuses") Collection<ConcertStatus> statuses);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :userId) AND c.startDate < :today AND c.status NOT IN :hidden")
    Page<Concert> findPastByUserCalendar(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT c FROM Concert c
            WHERE c.status NOT IN :hidden
            AND (
                LOWER(c.title) LIKE :q
                OR c.id IN (
                    SELECT ca.concertId FROM ConcertArtist ca
                    WHERE ca.artistId IN (
                        SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q
                    )
                    OR ca.artistId IN (
                        SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                    )
                )
            )
            ORDER BY c.startDate DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c) FROM Concert c
            WHERE c.status NOT IN :hidden
            AND (
                LOWER(c.title) LIKE :q
                OR c.id IN (
                    SELECT ca.concertId FROM ConcertArtist ca
                    WHERE ca.artistId IN (
                        SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q
                    )
                    OR ca.artistId IN (
                        SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                    )
                )
            )
            """)
    Page<Concert> searchConcerts(@Param("q") String q, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT c FROM Concert c
            WHERE c.status = :status
            AND (
                LOWER(c.title) LIKE :q
                OR c.id IN (
                    SELECT ca.concertId FROM ConcertArtist ca
                    WHERE ca.artistId IN (
                        SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q
                    )
                    OR ca.artistId IN (
                        SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                    )
                )
            )
            ORDER BY c.startDate DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c) FROM Concert c
            WHERE c.status = :status
            AND (
                LOWER(c.title) LIKE :q
                OR c.id IN (
                    SELECT ca.concertId FROM ConcertArtist ca
                    WHERE ca.artistId IN (
                        SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q
                    )
                    OR ca.artistId IN (
                        SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                    )
                )
            )
            """)
    Page<Concert> searchConcertsWithStatus(@Param("q") String q, @Param("status") ConcertStatus status, Pageable pageable);
}
