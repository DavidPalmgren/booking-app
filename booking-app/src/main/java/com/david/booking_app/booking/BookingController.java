package com.david.booking_app.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.david.booking_app.security.CurrentUserService;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookings;
    private final CurrentUserService currentUser;

    public BookingController(BookingService bookings, CurrentUserService currentUser) {
        this.bookings = bookings;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Booking> getAll(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long userId) {
        return bookings.find(roomId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody CreateBookingRequest request) {
        return bookings.book(
                currentUser.get(principal).getId(),
                request.roomId(),
                request.start(),
                request.end()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal OAuth2User principal, @PathVariable Long id) {
        bookings.cancel(id, currentUser.get(principal).getId());
    }

    public record CreateBookingRequest(
            Long roomId,
            LocalDateTime start,
            LocalDateTime end
    ) {}
}
