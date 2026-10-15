package com.david.booking_app.booking;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime tstart;
    private LocalDateTime tend;
    private Long roomId;
    private Long userId;

    protected Booking() {}

    public Booking(
            Long userId,
            Long roomId,
            LocalDateTime tstart,
            LocalDateTime tend) {

        this.userId = userId;
        this.roomId = roomId;
        this.tstart = tstart;
        this.tend = tend;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTstart() {
        return tstart;
    }

    public LocalDateTime getTend() {
        return tend;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getUserId() {
        return userId;
    }
}