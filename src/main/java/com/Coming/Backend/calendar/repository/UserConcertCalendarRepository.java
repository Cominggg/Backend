package com.Coming.Backend.calendar.repository;

import com.Coming.Backend.calendar.entity.UserConcertCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserConcertCalendarRepository extends JpaRepository<UserConcertCalendar, Long> {

    List<UserConcertCalendar> findByUserId(Long userId);

    Optional<UserConcertCalendar> findByUserIdAndConcertId(Long userId, Long concertId);

    boolean existsByUserIdAndConcertId(Long userId, Long concertId);

    void deleteByUserId(Long userId);
}
