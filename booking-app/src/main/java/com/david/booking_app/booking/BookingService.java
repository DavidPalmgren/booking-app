package com.david.booking_app.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.david.booking_app.common.ForbiddenException;
import com.david.booking_app.common.NotFoundException;
import com.david.booking_app.room.RoomRepository;
import com.david.booking_app.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

  private final BookingRepository repository;
  private final RoomRepository rooms;
  private final UserRepository users;

  public BookingService(BookingRepository repository, RoomRepository rooms, UserRepository users) {
    this.repository = repository;
    this.rooms = rooms;
    this.users = users;
  }

  public List<Booking> find(Long roomId, Long userId) {
    return repository.search(roomId, userId);
  }

  @Transactional
  public Booking book(Long userId, Long roomId, LocalDateTime start, LocalDateTime end) {
        if (userId == null || roomId == null || start == null || end == null) {
            throw new IllegalArgumentException(
                "userId, roomId, start and end are required"
            );
        }

        if (!users.existsById(userId)) {
            throw new NotFoundException("User " + userId + " not found");
        }

        if (!rooms.existsById(roomId)) {
            throw new NotFoundException("Room " + roomId + " not found");
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                "End time must be after start time"
            );
        }

        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Bookings cannot start in the past"
            );
        }

        if (repository.hasOverlap(roomId, start, end)) {
            throw new IllegalStateException(
                "Room is already booked for this time"
            );
        }

        Booking booking = new Booking(
            userId, roomId, start, end
        );

        return repository.save(booking);
  }

  @Transactional
  public void cancel(Long id, Long userId) {
    Booking booking = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));

    if (!booking.getUserId().equals(userId)) {
      throw new ForbiddenException("You can only cancel your own bookings");
    }
    repository.delete(booking);
  }
}
