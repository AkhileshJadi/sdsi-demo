package com.booking.service;

import com.booking.model.Booking;
import com.booking.model.Room;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Core booking engine. Thread-safe in-memory store. */
@Service
public class BookingService {

    private final List<Room> rooms = new ArrayList<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public BookingService(ObjectMapper mapper) { this.mapper = mapper; }

    @PostConstruct
    void loadRooms() throws Exception {
        try (InputStream in = new ClassPathResource("rooms.json").getInputStream()) {
            rooms.addAll(List.of(mapper.readValue(in, Room[].class)));
        }
    }

    // ---- M1: CRUD ----

    public Booking create(Booking b) {
        validate(b);
        bookings.put(b.getId(), b);
        return b;
    }

    public Optional<Booking> findById(String id) { return Optional.ofNullable(bookings.get(id)); }

    public List<Booking> listAll() { return List.copyOf(bookings.values()); }

    public Booking update(String id, Booking update) {
        Booking existing = findById(id).orElseThrow(() ->
                new IllegalArgumentException("Booking not found: " + id));
        bookings.remove(id);          // remove so overlap check skips self
        update.setId(id);
        validate(update);
        bookings.put(id, update);
        return update;
    }

    public void delete(String id) {
        if (bookings.remove(id) == null)
            throw new IllegalArgumentException("Booking not found: " + id);
    }

    // ---- M2 & M3: overlap + capacity validation ----

    void validate(Booking b) {
        Room r = rooms.stream().filter(x -> x.getName().equals(b.getRoom())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown room: " + b.getRoom()));
        if (b.getStart() == null || b.getEnd() == null)
            throw new IllegalArgumentException("Start and end are required");
        if (!b.getStart().isBefore(b.getEnd()))
            throw new IllegalArgumentException("Start must be before end");
        if (b.getAttendees() == null || b.getAttendees().isEmpty())
            throw new IllegalArgumentException("At least one attendee required");
        if (b.getAttendees().size() > r.getCapacity())
            throw new IllegalArgumentException(
                    "Attendees (%d) exceed capacity (%d)".formatted(b.getAttendees().size(), r.getCapacity()));
        // Business hours
        LocalTime startTime = b.getStart().toLocalTime();
        LocalTime endTime = b.getEnd().toLocalTime();
        if (startTime.isBefore(r.getOpenTime()) || endTime.isAfter(r.getCloseTime()))
            throw new IllegalArgumentException("Outside business hours 09:00–18:00");

        // Overlap: exists another booking where existing.start < b.end AND b.start < existing.end
        for (Booking other : bookings.values()) {
            if (other.getId().equals(b.getId())) continue;
            if (!other.getRoom().equals(b.getRoom())) continue;
            if (b.getStart().isBefore(other.getEnd()) && other.getStart().isBefore(b.getEnd()))
                throw new IllegalArgumentException(
                        "Overlap with booking %s (%s–%s)".formatted(other.getId(), other.getStart(), other.getEnd()));
        }
    }

    // ---- M4: find-slot ----

    /** Returns earliest free slot (room + start time) that fits the given duration and count on a date. */
    public Map<String, Object> findSlot(int durationMinutes, int attendeeCount, LocalDate date) {
        List<Room> candidates = rooms.stream()
                .filter(r -> r.getCapacity() >= attendeeCount)
                .sorted(Comparator.comparing(Room::getName))
                .toList();

        LocalTime dayStart = LocalTime.of(9, 0);
        LocalTime dayEnd   = LocalTime.of(18, 0);

        for (Room room : candidates) {
            // collect occupied intervals for this room on this date
            List<Slot> occupied = bookings.values().stream()
                    .filter(b -> b.getRoom().equals(room.getName()))
                    .filter(b -> b.getStart().toLocalDate().equals(date) || b.getEnd().toLocalDate().equals(date))
                    .map(b -> new Slot(b.getStart().toLocalTime(), b.getEnd().toLocalTime()))
                    .sorted()
                    .toList();

            LocalTime cursor = dayStart;
            for (Slot slot : occupied) {
                // gap from cursor to slot.start
                long gap = Duration.between(cursor, slot.start).toMinutes();
                if (gap >= durationMinutes && !cursor.plusMinutes(durationMinutes).isAfter(dayEnd)) {
                    LocalTime candidateStart = cursor;
                    if (!candidateStart.isBefore(dayStart) && !candidateStart.plusMinutes(durationMinutes).isAfter(dayEnd)) {
                        return Map.of(
                                "room", room.getName(),
                                "date", date.toString(),
                                "start", candidateStart.toString(),
                                "end", candidateStart.plusMinutes(durationMinutes).toString());
                    }
                }
                // advance cursor past this occupied slot
                LocalTime slotEndLimited = slot.end.isAfter(dayEnd) ? dayEnd : slot.end;
                if (slotEndLimited.isAfter(cursor)) cursor = slotEndLimited;
            }
            // final gap after last occupied
            long gap = Duration.between(cursor, dayEnd).toMinutes();
            if (gap >= durationMinutes && !cursor.plusMinutes(durationMinutes).isAfter(dayEnd)) {
                return Map.of(
                        "room", room.getName(),
                        "date", date.toString(),
                        "start", cursor.toString(),
                        "end", cursor.plusMinutes(durationMinutes).toString());
            }
        }
        throw new IllegalArgumentException("No free slot available");
    }

    // ---- M5: recurring bookings ----

    /** Creates weekly recurring bookings. Returns list of created bookings + conflicts. */
    public Map<String, Object> createRecurring(String room, String organizer, LocalDate firstDate,
                                                LocalTime startTime, LocalTime endTime,
                                                List<String> attendees, int weeks) {
        String group = UUID.randomUUID().toString();
        List<Booking> created = new ArrayList<>();
        List<Map<String, Object>> conflicts = new ArrayList<>();

        for (int w = 0; w < weeks; w++) {
            LocalDate date = firstDate.plusWeeks(w);
            LocalDateTime start = LocalDateTime.of(date, startTime);
            LocalDateTime end   = LocalDateTime.of(date, endTime);
            Booking b = new Booking(room, organizer, start, end, attendees);
            b.setRecurrenceGroup(group);
            try {
                bookings.put(b.getId(), b);
                validate(b);
                created.add(b);
            } catch (IllegalArgumentException ex) {
                bookings.remove(b.getId());
                conflicts.add(Map.of("date", date.toString(), "reason", ex.getMessage()));
            }
        }
        return Map.of("group", group, "created", created, "conflicts", conflicts);
    }

    public List<Room> getRooms() { return List.copyOf(rooms); }

    // ---- internal helpers ----

    private record Slot(LocalTime start, LocalTime end) implements Comparable<Slot> {
        public int compareTo(Slot o) { return start.compareTo(o.start); }
    }
}