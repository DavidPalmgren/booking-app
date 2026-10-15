package com.david.booking_app.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.roomId = :roomId
          AND b.tstart < :end
          AND b.tend > :start
    """)
    boolean hasOverlap(
        @Param("roomId") Long roomId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT b
        FROM Booking b
        WHERE (:roomId IS NULL OR b.roomId = :roomId)
          AND (:userId IS NULL OR b.userId = :userId)
        ORDER BY b.tstart
    """)
    List<Booking> search(
        @Param("roomId") Long roomId,
        @Param("userId") Long userId
    );
}
