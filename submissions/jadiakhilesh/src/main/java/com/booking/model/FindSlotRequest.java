package com.booking.model;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Request body for the find-slot endpoint (M4). */
public class FindSlotRequest {
    @Positive private int durationMinutes;
    @Positive private int attendeeCount;
    @NotNull  private LocalDate date;

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getAttendeeCount() { return attendeeCount; }
    public void setAttendeeCount(int attendeeCount) { this.attendeeCount = attendeeCount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}