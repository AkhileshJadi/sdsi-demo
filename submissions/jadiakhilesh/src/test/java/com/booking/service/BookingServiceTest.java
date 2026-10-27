package com.booking.service;

import com.booking.model.Booking;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.MethodName.class)
class BookingServiceTest {

    static BookingService svc;

    @BeforeAll
    static void init() throws Exception {
        svc = new BookingService(new ObjectMapper());
        svc.loadRooms();
    }

    @BeforeEach
    void clear() {
        // remove all existing bookings except room data
        for (Booking b : svc.listAll()) svc.delete(b.getId());
    }

    // ---- M1: CRUD ----

    @Test
    void m1a_createAndRead() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com", "b@x.com"));
        Booking created = svc.create(b);
        assertNotNull(created.getId());
        assertEquals("alice", svc.findById(created.getId()).orElseThrow().getOrganizer());
    }

    @Test
    void m1b_listAll() {
        svc.create(new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com")));
        assertEquals(1, svc.listAll().size());
    }

    @Test
    void m1c_update() {
        Booking b = svc.create(new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com")));
        Booking upd = new Booking("Room 1", "bob", dt("2026-10-28T14:00"), dt("2026-10-28T15:00"),
                List.of("b@x.com"));
        Booking result = svc.update(b.getId(), upd);
        assertEquals("bob", result.getOrganizer());
    }

    @Test
    void m1d_delete() {
        Booking b = svc.create(new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com")));
        svc.delete(b.getId());
        assertTrue(svc.findById(b.getId()).isEmpty());
    }

    @Test
    void m1e_deleteNotFoundThrows() {
        assertThrows(IllegalArgumentException.class, () -> svc.delete("nonexistent"));
    }

    // ---- M2: Overlap rejection ----

    @Test
    void m2a_rejectOverlap() {
        svc.create(new Booking("Room 2", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:30"),
                List.of("a@x.com")));
        Booking overlap = new Booking("Room 2", "bob", dt("2026-10-28T11:00"), dt("2026-10-28T12:00"),
                List.of("b@x.com"));
        assertThrows(IllegalArgumentException.class, () -> svc.create(overlap));
    }

    @Test
    void m2b_allowBackToBack() {
        svc.create(new Booking("Room 2", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com")));
        Booking b2b = new Booking("Room 2", "bob", dt("2026-10-28T11:00"), dt("2026-10-28T12:00"),
                List.of("b@x.com"));
        assertDoesNotThrow(() -> svc.create(b2b));
    }

    // ---- M3: Capacity ----

    @Test
    void m3a_rejectOverCapacity() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com", "b@x.com", "c@x.com", "d@x.com", "e@x.com")); // 5 > 4
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void m3b_allowWithinCapacity() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com", "b@x.com", "c@x.com")); // 3 <= 4
        assertDoesNotThrow(() -> svc.create(b));
    }

    // ---- M4: find-slot ----

    @Test
    void m4a_emptyDayReturns9am() {
        var slot = svc.findSlot(30, 2, LocalDate.of(2026, 11, 1));
        assertEquals("09:00", slot.get("start"));
    }

    @Test
    void m4b_skipOccupied() {
        // book 09:00-10:00, find-slot 30min should return 10:00
        svc.create(new Booking("Room 1", "alice", dt("2026-12-01T09:00"), dt("2026-12-01T10:00"),
                List.of("a@x.com")));
        var slot = svc.findSlot(30, 2, LocalDate.of(2026, 12, 1));
        assertEquals("10:00", slot.get("start"));
    }

    @Test
    void m4c_noRoomLargeEnoughThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.findSlot(30, 50, LocalDate.of(2026, 11, 1)));
    }

    @Test
    void m4d_noGapLongEnoughThrows() {
        // book the whole day across all rooms
        for (String room : List.of("Room 1","Room 2","Room 3","Room 4","Room 5")) {
            svc.create(new Booking(room, "alice", dt("2026-12-15T09:00"), dt("2026-12-15T18:00"),
                    List.of("a@x.com")));
        }
        assertThrows(IllegalArgumentException.class,
                () -> svc.findSlot(30, 1, LocalDate.of(2026, 12, 15)));
    }

    // ---- M5: Recurring ----

    @Test
    void m5a_createsRecurringBookings() {
        var result = svc.createRecurring("Room 2", "alice",
                LocalDate.of(2026, 11, 2), // Monday
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                List.of("a@x.com"), 4);
        @SuppressWarnings("unchecked")
        List<Booking> created = (List<Booking>) result.get("created");
        assertEquals(4, created.size());
        assertNotNull(created.get(0).getRecurrenceGroup());
        assertEquals(created.get(0).getRecurrenceGroup(), created.get(1).getRecurrenceGroup());
    }

    @Test
    void m5b_recurringReportsConflicts() {
        // book week 2 manually, then try recurring → week 2 should conflict
        svc.create(new Booking("Room 2", "bob",
                dt("2026-11-09T10:00"), dt("2026-11-09T11:00"),
                List.of("b@x.com")));
        var result = svc.createRecurring("Room 2", "alice",
                LocalDate.of(2026, 11, 2),
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                List.of("a@x.com"), 2);
        @SuppressWarnings("unchecked")
        List<?> conflicts = (List<?>) result.get("conflicts");
        assertEquals(1, conflicts.size());
        @SuppressWarnings("unchecked")
        List<Booking> created = (List<Booking>) result.get("created");
        assertEquals(1, created.size()); // only week 1 succeeded
    }

    // ---- Edge cases ----

    @Test
    void edge_startNotBeforeEnd() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T11:00"), dt("2026-10-28T10:00"),
                List.of("a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void edge_sameStartEndThrows() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T10:00"),
                List.of("a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void edge_beforeBusinessHours() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T08:00"), dt("2026-10-28T09:30"),
                List.of("a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void edge_afterBusinessHours() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T17:30"), dt("2026-10-28T18:30"),
                List.of("a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void edge_unknownRoomThrows() {
        Booking b = new Booking("Mars", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void edge_zeroAttendees() {
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of());
        assertThrows(IllegalArgumentException.class, () -> svc.create(b));
    }

    @Test
    void edge_exactlyOnBoundary() {
        // end at 18:00 is allowed, back-to-back at 18:00 ends at 18:00
        Booking b = new Booking("Room 1", "alice", dt("2026-10-28T17:00"), dt("2026-10-28T18:00"),
                List.of("a@x.com"));
        assertDoesNotThrow(() -> svc.create(b));
    }

    @Test
    void edge_updateOntoSelfIgnored() {
        Booking b = svc.create(new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com")));
        // Updating itself with same time should be allowed (self not checked)
        Booking upd = new Booking("Room 1", "alice", dt("2026-10-28T10:00"), dt("2026-10-28T11:00"),
                List.of("a@x.com"));
        assertDoesNotThrow(() -> svc.update(b.getId(), upd));
    }

    // ---- helper ----

    static LocalDateTime dt(String iso) { return LocalDateTime.parse(iso); }
}