package com.booking.controller;

import com.booking.model.*;
import com.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST controller for all booking operations. */
@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService svc;

    public BookingController(BookingService svc) { this.svc = svc; }

    // ---- M1 ----

    @PostMapping("/bookings")
    public ResponseEntity<?> create(@Valid @RequestBody Booking b) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(svc.create(b));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bookings")
    public List<Booking> list() { return svc.listAll(); }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return svc.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/bookings/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody Booking b) {
        try {
            return ResponseEntity.ok(svc.update(id, b));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            svc.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- M4 ----

    @PostMapping("/find-slot")
    public ResponseEntity<?> findSlot(@Valid @RequestBody FindSlotRequest req) {
        try {
            return ResponseEntity.ok(svc.findSlot(
                    req.getDurationMinutes(), req.getAttendeeCount(), req.getDate()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- M5 ----

    @PostMapping("/bookings/recurring")
    public ResponseEntity<?> recurring(@Valid @RequestBody RecurringBookingRequest req) {
        try {
            var result = svc.createRecurring(
                    req.getRoom(), req.getOrganizer(), req.getFirstDate(),
                    req.getTime().getStart(), req.getTime().getEnd(),
                    req.getAttendees(), req.getWeeks());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- rooms ----

    @GetMapping("/rooms")
    public List<Room> rooms() { return svc.getRooms(); }
}