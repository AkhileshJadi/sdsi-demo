package com.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalTime;
import java.util.Objects;

/** Immutable room definition loaded from rooms.json. */
public class Room {
    private String name;
    private int capacity;

    public Room() {}

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /** Business hours start (inclusive). */
    @JsonIgnore
    public LocalTime getOpenTime() { return LocalTime.of(9, 0); }

    /** Business hours end (inclusive — last booking may end at 18:00). */
    @JsonIgnore
    public LocalTime getCloseTime() { return LocalTime.of(18, 0); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Room r)) return false;
        return Objects.equals(name, r.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}