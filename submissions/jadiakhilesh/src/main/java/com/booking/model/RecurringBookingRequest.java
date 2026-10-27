package com.booking.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Request body for creating a recurring booking (M5). */
public class RecurringBookingRequest {
    @NotBlank  private String room;
    @NotBlank  private String organizer;
    @NotNull   private LocalDate firstDate;
    @NotNull   @Valid private TimeRange time;
    @NotEmpty  private List<@Email String> attendees;
    @Min(1) @Max(52) private int weeks = 4; // default 4 weeks

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public LocalDate getFirstDate() { return firstDate; }
    public void setFirstDate(LocalDate firstDate) { this.firstDate = firstDate; }
    public TimeRange getTime() { return time; }
    public void setTime(TimeRange time) { this.time = time; }
    public List<String> getAttendees() { return attendees; }
    public void setAttendees(List<String> attendees) { this.attendees = attendees; }
    public int getWeeks() { return weeks; }
    public void setWeeks(int weeks) { this.weeks = weeks; }

    /** Nested time-range for recurring bookings. */
    public static class TimeRange {
        @NotNull private java.time.LocalTime start;
        @NotNull private java.time.LocalTime end;
        public java.time.LocalTime getStart() { return start; }
        public void setStart(java.time.LocalTime start) { this.start = start; }
        public java.time.LocalTime getEnd() { return end; }
        public void setEnd(java.time.LocalTime end) { this.end = end; }
    }
}