package dev.kaffi.studymate.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionServiceTest {

	private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
	private static final Instant SAFE_DATE = Instant.parse("2026-03-15T09:00:00Z");

	private MutableClock clock;
	private InMemoryStorageManager storageManager;
	private SessionService service;

	@BeforeEach
	void setup() {
		clock = new MutableClock(SAFE_DATE, PRAGUE);
		storageManager = new InMemoryStorageManager();
		service = new SessionService(clock, storageManager);
	}

	// ====================
	// service construction
	// ====================

	@Test
	@DisplayName("A service cannot be built without a clock")
	void constructor_rejectsNullClock() {
		assertThrows(NullPointerException.class, () -> new SessionService(null, storageManager));
	}

	@Test
	@DisplayName("A service cannot be built without a storage manager")
	void constructor_rejectsNullStorageManager() {
		assertThrows(NullPointerException.class, () -> new SessionService(clock, null));
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

	@Test
	@DisplayName("Starting a session persists it")
	void startSession_persistsRunningSession() {
		RunningSession session = service.startSession("Java Generics");

		assertEquals(Optional.of(session), storageManager.getRunningSession());
	}

	@Test
	@DisplayName("Starting a session stores nothing in the completed session store")
	void startSession_storesNoCompletedSession() {
		service.startSession("Java Generics");

		assertEquals(List.of(), storageManager.allCompletedSessions());
	}

	@Test
	@DisplayName("Starting a second session while one is running is rejected")
	void startSession_rejectsSecondSessionWhileOneIsRunning() {
		service.startSession("Java Generics");

		assertThrows(SessionAlreadyRunningException.class, () -> service.startSession("Java Streams"));
	}

	@Test
	@DisplayName("A rejected second session does not replace the running one")
	void startSession_keepsFirstSessionAfterRejection() {
		RunningSession session = service.startSession("Java Generics");
		clock.advance(Duration.ofMinutes(5));

		assertThrows(SessionAlreadyRunningException.class, () -> service.startSession("Java Streams"));
		assertEquals(Optional.of(session), storageManager.getRunningSession());
	}

	@Test
	@DisplayName("A null topic is rejected")
	void startSession_rejectsNullTopic() {
		assertThrows(NullPointerException.class, () -> service.startSession(null));
	}

	@ParameterizedTest(name = "topic = \"{0}\"")
	@ValueSource(strings = {"", "   ", "Java\tGenerics"})
	@DisplayName("An invalid topic is rejected")
	void startSession_rejectsInvalidTopic(String invalid) {
		assertThrows(IllegalArgumentException.class, () -> service.startSession(invalid));
	}

	@Test
	@DisplayName("A rejected topic leaves nothing stored")
	void startSession_storesNothingWhenTopicIsInvalid() {
		assertThrows(IllegalArgumentException.class, () -> service.startSession(""));
		assertEquals(Optional.empty(), storageManager.getRunningSession());
	}

	// ====================
	// stopCurrentSession
	// ====================

	@Test
	@DisplayName("Ending a session preserves the topic and start instant")
	void endSession_preservesTopicAndStartInstant() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.stopCurrentSession().get();

		assertAll(
				() -> assertEquals(new Topic("Java Generics"), completed.topic()),
				() -> assertEquals(SAFE_DATE, completed.start())
		);
	}

	@Test
	@DisplayName("The end instant comes from the clock at the moment of stopping")
	void endSession_capturesEndInstantFromClock() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.stopCurrentSession().get();

		assertEquals(SAFE_DATE.plus(Duration.ofHours(2)), completed.end());
	}

	@Test
	@DisplayName("Duration reflects the time that elapsed between start and stop")
	void endSession_durationReflectsElapsedTime() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.stopCurrentSession().get();

		assertEquals(Duration.ofHours(2), completed.duration());
	}

	@Test
	@DisplayName("A session stopped immediately has zero duration")
	void endSession_allowsZeroLengthSession() {
		service.startSession("Java Generics");
		CompletedSession completed = service.stopCurrentSession().get();

		assertEquals(Duration.ZERO, completed.duration());
	}

	@Test
	@DisplayName("Ending a session persists it to the completed session store")
	void endSession_persistsCompletedSession() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		CompletedSession completed = service.stopCurrentSession().get();

		assertEquals(List.of(completed), storageManager.allCompletedSessions());
	}

	@Test
	@DisplayName("Ending a session clears the running session")
	void endSession_clearsRunningSession() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));

		service.stopCurrentSession();

		assertEquals(Optional.empty(), storageManager.getRunningSession());
	}

	@Test
	@DisplayName("A new session can be started once the previous one has ended")
	void endSession_allowsANewSessionAfterwards() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));
		service.stopCurrentSession();

		RunningSession second = service.startSession("Java Streams");

		assertEquals(Optional.of(second), storageManager.getRunningSession());
	}

	@Test
	@DisplayName("Duration handles elapsed time across a DST transition")
	void endSession_durationIsUnaffectedByDstTransition() {
		clock.setTo(Instant.parse("2026-10-25T00:30:00Z"));

		service.startSession("Java Generics");
		clock.advance(Duration.ofMinutes(120));
		CompletedSession completed = service.stopCurrentSession().get();

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

	@Test
	@DisplayName("Stopping when nothing is running returns empty")
	void endSession_returnsEmptyWhenNothingRunning() {
		assertEquals(Optional.empty(), service.stopCurrentSession());
	}

	@Test
	@DisplayName("Stopping when nothing is running stores no completed session")
	void endSession_storesNoCompletedSessionWhenNothingRunning() {
		service.stopCurrentSession();

		assertEquals(List.of(), storageManager.allCompletedSessions());
	}

	// ====================
	// getCurrentSession
	// ====================

	@Test
	@DisplayName("There is no current session before one is started")
	void getCurrentSession_returnsEmptyWhenNothingRunning() {
		assertEquals(Optional.empty(), service.getCurrentSession());
	}

	@Test
	@DisplayName("The current session is the one that was started")
	void getCurrentSession_returnsTheRunningSession() {
		RunningSession started = service.startSession("Java Generics");

		assertEquals(Optional.of(started), service.getCurrentSession());
	}

	@Test
	@DisplayName("There is no current session once it has been stopped")
	void getCurrentSession_returnsEmptyAfterStopping() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofHours(2));
		service.stopCurrentSession();

		assertEquals(Optional.empty(), service.getCurrentSession());
	}

	// ====================
	// elapsedTime
	// ====================

	@Test
	@DisplayName("Elapsed time is empty when nothing is running")
	void elapsedTime_returnsEmptyWhenNothingRunning() {
		assertEquals(Optional.empty(), service.elapsedTime());
	}

	@Test
	@DisplayName("Elapsed time is zero immediately after starting")
	void elapsedTime_isZeroImmediatelyAfterStart() {
		service.startSession("Java Generics");

		assertEquals(Optional.of(Duration.ZERO), service.elapsedTime());
	}

	@Test
	@DisplayName("Elapsed time reflects the time since the session started")
	void elapsedTime_reflectsTimeSinceStart() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofMinutes(45));

		assertEquals(Optional.of(Duration.ofMinutes(45)), service.elapsedTime());
	}

	@Test
	@DisplayName("Elapsed time is empty again once the session has been stopped")
	void elapsedTime_returnsEmptyAfterStopping() {
		service.startSession("Java Generics");
		clock.advance(Duration.ofMinutes(45));
		service.stopCurrentSession();

		assertEquals(Optional.empty(), service.elapsedTime());
	}
}