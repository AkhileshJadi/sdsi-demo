# Meeting Room Booking Engine

REST API for office room booking. Spring Boot 3.5, Java 25, in-memory storage.

## Run

```bash
mvn spring-boot:run
# → starts on http://localhost:8080
```

## Test

```bash
mvn test
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST`   | `/api/bookings`          | Create booking (M1) |
| `GET`    | `/api/bookings`          | List all bookings (M1) |
| `GET`    | `/api/bookings/{id}`     | Get booking by ID (M1) |
| `PUT`    | `/api/bookings/{id}`     | Update booking (M1) |
| `DELETE` | `/api/bookings/{id}`     | Delete booking (M1) |
| `POST`   | `/api/find-slot`         | Find earliest free slot (M4) |
| `POST`   | `/api/bookings/recurring`| Create weekly recurring bookings (M5) |
| `GET`    | `/api/rooms`             | List available rooms |

### Booking JSON

```json
{
  "room": "Room 1",
  "organizer": "alice",
  "start": "2026-11-01T09:00:00",
  "end":   "2026-11-01T10:00:00",
  "attendees": ["a@x.com", "b@x.com"]
}
```

### Find-slot JSON

```json
{
  "durationMinutes": 30,
  "attendeeCount": 2,
  "date": "2026-11-01"
}
```

### Recurring JSON

```json
{
  "room": "Room 2",
  "organizer": "alice",
  "firstDate": "2026-11-02",
  "time": {"start": "10:00", "end": "11:00"},
  "attendees": ["a@x.com"],
  "weeks": 4
}
```

## Rooms (seeded from `rooms.json`)

| Room | Capacity |
|------|----------|
| Room 1 | 4 |
| Room 2 | 6 |
| Room 3 | 10 |
| Room 4 | 2 |
| Room 5 | 20 |

## Requirements Checklist

| # | Requirement | Where |
|---|-------------|-------|
| M1 | CRUD bookings | BookingService.create/update/delete/findById, BookingController |
| M2 | Overlap rejection (back-to-back allowed) | BookingService.validate() overlap loop |
| M3 | Capacity check | BookingService.validate() attendee vs room capacity |
| M4 | Find-slot (earliest room+time) | BookingService.findSlot() |
| M5 | Weekly recurring with conflict reporting | BookingService.createRecurring() |
| — | Business hours 09:00–18:00 | Room.getOpenTime/getCloseTime + validate() |
| — | Input validation | @Valid, @NotBlank, @NotNull, @Email on models |
| — | Unknown room rejected | validate() room lookup |
| — | Global error handling | GlobalExceptionHandler |
| — | Tests (23 test cases) | BookingServiceTest |

## Assumptions

- All times are local server time; no timezone handling.
- Back-to-back means booking A ends at 10:00 → booking B starts at 10:00 is allowed.
- Recurring bookings use `weeks` (1–52); daily recurrence not supported.
- Find-slot searches rooms alphabetically and returns the first fit.
- A booking ending exactly at 18:00 is allowed (starts before 18:00).