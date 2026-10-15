package com.david.booking_app.room;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.david.booking_app.common.NotFoundException;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomRepository rooms;

    public RoomController(RoomRepository rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room create(@RequestBody CreateRoomRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Room name is required");
        }

        Room room = new Room(
                request.name().trim()
        );

        return rooms.save(room);
    }

    @GetMapping
    public List<Room> getAll() {
        return rooms.findAll();
    }

    @GetMapping("/{id}")
    public Room getRoom(@PathVariable Long id) {
        return rooms.findById(id)
                .orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
    }

    public record CreateRoomRequest(
            String name
    ) {}
}
