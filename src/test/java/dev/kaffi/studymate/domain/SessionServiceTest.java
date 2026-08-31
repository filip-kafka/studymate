package dev.kaffi.studymate.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionServiceTest {

	private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
	private static final Instant SAFE_DATE = Instant.parse("2026-03-15T09:00:00Z");

	private MutableClock clock;
	private SessionService service;

	@BeforeEach
	void setup() {
		clock = new MutableClock(SAFE_DATE, PRAGUE);
		service = new SessionService(clock);
	}

	// ====================
	// service construction
	// ====================

	@Test
	@DisplayName("A service cannot be built without a clock")
	void constructor_rejectsNullClock() {
		assertThrows(NullPointerException.class, () -> new SessionService(null));
	}

	// ====================
	// startSession
	// ====================

	@Test
	@DisplayName("Starting a session records the topic and the clock's instant")
	void startSession_capturesTopicAndStartInstant() {
		RunningSession running = service.startSession("Java Generics");

		assertAll(
				() -> assertEquals(new Topic("Java Generics"), running.topic()),
				() -> assertEquals(SAFE_DATE, running.start())
		);
	}

	// ====================
	// endSession
	// ====================

	@Test
	@DisplayName("Ending a session preserves the topic and start instant")
	void endSession_preservesTopicAndStartInstant() {
		RunningSession running = service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.endSession(running);

		assertAll(
				() -> assertEquals(new Topic("Java Generics"), completed.topic()),
				() -> assertEquals(SAFE_DATE, completed.start())
		);
	}

	@Test
	@DisplayName("The end instant comes from the clock at the moment of stopping")
	void endSession_capturesEndInstantFromClock() {
		RunningSession running = service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.endSession(running);

		assertEquals(SAFE_DATE.plus(Duration.ofHours(2)), completed.end());
	}

	@Test
	@DisplayName("Duration reflects the time that elapsed between start and stop")
	void endSession_durationReflectsElapsedTime() {
		RunningSession running = service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.endSession(running);

		assertEquals(Duration.ofHours(2), completed.duration());
	}

	@Test
	@DisplayName("A session stopped immediately has zero duration")
	void endSession_allowsZeroLengthSession() {
		RunningSession running = service.startSession("Java Generics");
		CompletedSession completed = service.endSession(running);

		assertEquals(Duration.ZERO, completed.duration());
	}

	@Test
	@DisplayName("A session spanning midnight is handled as one continuous interval")
	void endSession_handlesSessionSpanningMidnight() {
		clock.setTo(Instant.parse("2026-03-15T22:00:00Z"));
		RunningSession running = service.startSession("Java Generics");
		clock.advance(Duration.ofHours(3));

		CompletedSession completed = service.endSession(running);

		assertEquals(Duration.ofHours(3), completed.duration());
	}

	@Test
	@DisplayName("A null running session is rejected")
	void endSession_rejectsNullSession() {
		assertThrows(NullPointerException.class, () -> service.endSession(null));
	}

	@Test
	@DisplayName("Duration handles elapsed time across a DST transition")
	void endSession_durationIsUnaffectedByDstTransition() {
		clock.setTo(Instant.parse("2026-10-25T00:30:00Z")); // DST transition day in Czechia, at 2:30 AM

		RunningSession running = service.startSession("Java Generics");
		clock.advance(Duration.ofMinutes(120));
		CompletedSession completed = service.endSession(running);

		ZonedDateTime localStart = completed.start().atZone(PRAGUE);
		ZonedDateTime localEnd = completed.end().atZone(PRAGUE);

		assertAll(
				() -> assertEquals(Duration.ofMinutes(120), completed.duration(),
						"real elapsed time, not wall-clock difference"),
				() -> assertEquals(2, localStart.getHour(), "local start hour"),
				() -> assertEquals(30, localStart.getMinute(), "local start minute"),
				() -> assertEquals(3, localEnd.getHour(), "local end hour"),
				() -> assertEquals(30, localEnd.getMinute(), "local end minute")
		);
	}
}