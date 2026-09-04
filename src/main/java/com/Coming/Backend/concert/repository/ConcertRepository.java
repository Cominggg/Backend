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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Optional<Concert> findByKopisId(String kopisId);

    boolean existsByKopisId(String kopisId);

    boolean existsByTitleAndStartDateAndEndDate(String title, LocalDate startDate, LocalDate endDate);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.startDate >= :since AND c.status NOT IN :hidden")
    Page<Concert> findAllByArtistId(@Param("artistId") Long artistId, @Param("since") LocalDate since, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.status IN :statuses AND c.startDate >= :since")
    Page<Concert> findAllByArtistIdAndStatusIn(@Param("artistId") Long artistId, @Param("statuses") List<ConcertStatus> statuses, @Param("since") LocalDate since, Pageable pageable);

    Page<Concert> findByStatus(ConcertStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Concert c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT c FROM Concert c WHERE c.startDate <= :lastDay AND c.endDate >= :firstDay AND c.status NOT IN :hidden ORDER BY c.startDate ASC")
    List<Concert> findByDateRange(@Param("firstDay") LocalDate firstDay, @Param("lastDay") LocalDate lastDay, @Param("hidden") Collection<ConcertStatus> hidden);

    @Query("""
            SELECT c FROM Concert c
            WHERE c.status NOT IN :hidden
            ORDER BY
                CASE WHEN c.status = com.Coming.Backend.concert.entity.ConcertStatus.UPCOMING THEN 0
                     WHEN c.status = com.Coming.Backend.concert.entity.ConcertStatus.ONGOING  THEN 0
                     WHEN c.status = com.Coming.Backend.concert.entity.ConcertStatus.ENDED    THEN 1
                     ELSE 2
                END ASC,
                c.viewCount DESC
            LIMIT 10
            """)
    List<Concert> findTop10Popular(@Param("hidden") Collection<ConcertStatus> hidden);

    @Query("SELECT COUNT(c) FROM Concert c WHERE YEAR(c.startDate) = :year AND MONTH(c.startDate) = :month")
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(c) > 0 FROM Concert c WHERE c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId = :artistId) AND c.status IN :statuses")
    boolean existsActiveByArtistId(@Param("artistId") Long artistId, @Param("statuses") Collection<ConcertStatus> statuses);

    @Query("SELECT c FROM Concert c WHERE c.ticketOpenAt >= :monthStart AND c.ticketOpenAt < :monthEnd AND c.status NOT IN :hidden ORDER BY c.ticketOpenAt ASC")
    List<Concert> findByTicketOpenAtRange(@Param("monthStart") LocalDateTime monthStart, @Param("monthEnd") LocalDateTime monthEnd, @Param("hidden") Collection<ConcertStatus> hidden);

    @Query("SELECT c FROM Concert c WHERE c.ticketOpenAt > :now AND c.status NOT IN :hidden ORDER BY c.ticketOpenAt ASC")
    List<Concert> findUpcomingTicketing(@Param("now") LocalDateTime now, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.ticketOpenAt > :now AND c.status NOT IN :hidden AND c.id IN (SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :artistIds) ORDER BY c.ticketOpenAt ASC")
    List<Concert> findUpcomingTicketingByArtistIds(@Param("now") LocalDateTime now, @Param("hidden") Collection<ConcertStatus> hidden, @Param("artistIds") List<Long> artistIds, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :userId) AND c.endDate >= :today AND c.status NOT IN :hidden")
    Page<Concert> findUpcomingByUserCalendar(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.id IN (SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :userId) AND c.endDate < :today AND c.status NOT IN :hidden")
    Page<Concert> findPastByUserCalendar(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("hidden") Collection<ConcertStatus> hidden, Pageable pageable);

    /**
     * status·inCalendar·followedOnly·ticketOpenPending 필터를 조합해 공연 목록을 조회한다 (q 없음).
     * 각 필터는 대응 파라미터가 null(또는 false)이면 조건 없이 통과된다.
     */
    @Query(value = """
            SELECT c FROM Concert c
            WHERE c.status NOT IN :hidden
            AND (:status IS NULL OR c.status = :status)
            AND (:calendarUserId IS NULL OR c.id IN (
                SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :calendarUserId
            ))
            AND (:followedArtistIds IS NULL OR c.id IN (
                SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :followedArtistIds
            ))
            AND (:ticketOpenPending = false OR (c.ticketOpenAt IS NOT NULL AND c.ticketOpenAt > :now))
            """,
            countQuery = """
            SELECT COUNT(c) FROM Concert c
            WHERE c.status NOT IN :hidden
            AND (:status IS NULL OR c.status = :status)
            AND (:calendarUserId IS NULL OR c.id IN (
                SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :calendarUserId
            ))
            AND (:followedArtistIds IS NULL OR c.id IN (
                SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :followedArtistIds
            ))
            AND (:ticketOpenPending = false OR (c.ticketOpenAt IS NOT NULL AND c.ticketOpenAt > :now))
            """)
    Page<Concert> findConcerts(@Param("hidden") Collection<ConcertStatus> hidden,
                                @Param("status") ConcertStatus status,
                                @Param("calendarUserId") Long calendarUserId,
                                @Param("followedArtistIds") List<Long> followedArtistIds,
                                @Param("ticketOpenPending") boolean ticketOpenPending,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

    /**
     * status·inCalendar·followedOnly·ticketOpenPending 필터에 공연명·아티스트명(alias 포함) 텍스트 검색을 더해 조회한다 (q 있음).
     */
    @Query(value = """
            SELECT DISTINCT c FROM Concert c
            WHERE c.status NOT IN :hidden
            AND (:status IS NULL OR c.status = :status)
            AND (:calendarUserId IS NULL OR c.id IN (
                SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :calendarUserId
            ))
            AND (:followedArtistIds IS NULL OR c.id IN (
                SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :followedArtistIds
            ))
            AND (:ticketOpenPending = false OR (c.ticketOpenAt IS NOT NULL AND c.ticketOpenAt > :now))
            AND (
                LOWER(c.title) LIKE :q
                OR c.id IN (
                    SELECT ca2.concertId FROM ConcertArtist ca2
                    WHERE ca2.artistId IN (
                        SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q OR LOWER(a.sortName) LIKE :q
                    )
                    OR ca2.artistId IN (
                        SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                    )
                )
            )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c) FROM Concert c
            WHERE c.status NOT IN :hidden
            AND (:status IS NULL OR c.status = :status)
            AND (:calendarUserId IS NULL OR c.id IN (
                SELECT ucc.concertId FROM UserConcertCalendar ucc WHERE ucc.userId = :calendarUserId
            ))
            AND (:followedArtistIds IS NULL OR c.id IN (
                SELECT ca.concertId FROM ConcertArtist ca WHERE ca.artistId IN :followedArtistIds
            ))
            AND (:ticketOpenPending = false OR (c.ticketOpenAt IS NOT NULL AND c.ticketOpenAt > :now))
            AND (
                LOWER(c.title) LIKE :q
                OR c.id IN (
                    SELECT ca2.concertId FROM ConcertArtist ca2
                    WHERE ca2.artistId IN (
                        SELECT a.id FROM Artist a WHERE LOWER(a.name) LIKE :q OR LOWER(a.sortName) LIKE :q
                    )
                    OR ca2.artistId IN (
                        SELECT al.artistId FROM ArtistAlias al WHERE LOWER(al.name) LIKE :q
                    )
                )
            )
            """)
    Page<Concert> searchConcerts(@Param("hidden") Collection<ConcertStatus> hidden,
                                  @Param("status") ConcertStatus status,
                                  @Param("calendarUserId") Long calendarUserId,
                                  @Param("followedArtistIds") List<Long> followedArtistIds,
                                  @Param("ticketOpenPending") boolean ticketOpenPending,
                                  @Param("now") LocalDateTime now,
                                  @Param("q") String q,
                                  Pageable pageable);
}
