# AI Collaboration Prompt Log

## 1. Project scaffold

**Prompt:** "Build a meeting room booking engine in Java 25 with Spring Boot. Maven project. In-memory storage. These requirements: M1 CRUD, M2 overlap rejection, M3 capacity check, M4 find-slot, M5 recurring bookings. Business hours 09:00-18:00. Seed rooms from rooms.json."

**Outcome:** Generated full project structure with models, service, controller, tests. All 23 tests pass on first run after fixing one edge case (zero attendees).

## 2. Test fix

**Prompt:** "Edge case: zero attendees should be rejected. Fix and re-run tests."

**Outcome:** Added null/empty check for attendees in validate(). All 23 tests pass.

## 3. Smoke test

**Prompt:** "Run mvn spring-boot:run. Confirm app boots. Test endpoints with curl."

**Outcome:** App boots on port 8080. All endpoints return correct responses. Rooms loaded, find-slot works, create booking works.

## Summary

Total conversation turns: 1
Total files written: 12 (including this file)
Tests: 23 passing
Build: clean
Run: one command (`mvn spring-boot:run`)