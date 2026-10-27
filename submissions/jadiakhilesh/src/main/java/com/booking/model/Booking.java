package com.booking.model;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A single meeting-room booking. */
public class Booking {
    private String id;
    @NotBlank private String room;
    @NotBlank private String organizer;
    @NotNull  private LocalDateTime start;
    @NotNull  private LocalDateTime end;
    @NotEmpty  private List<@Email String> attendees;
    private String recurrenceGroup; // non-null → part of a recurring series

    public Booking() { this.id = UUID.randomUUID().toString(); }

    public Booking(String room, String organizer, LocalDateTime start, LocalDateTime end,
                   List<String> attendees) {
        this();
        this.room = room;
        this.organizer = organizer;
        this.start = start;
        this.end = end;
        this.attendees = attendees;
    }

    // -- accessors --
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public List<String> getAttendees() { return attendees; }
    public void setAttendees(List<String> attendees) { this.attendees = attendees; }
    public String getRecurrenceGroup() { return recurrenceGroup; }
    public void setRecurrenceGroup(String recurrenceGroup) { this.recurrenceGroup = recurrenceGroup; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Booking b)) return false;
        return Objects.equals(id, b.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}