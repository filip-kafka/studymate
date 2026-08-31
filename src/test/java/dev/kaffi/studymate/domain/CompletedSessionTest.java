package dev.kaffi.studymate.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CompletedSessionTest {

	private static final Instant SAFE_DATE = Instant.parse("2026-03-15T09:00:00Z");

	@Test
	@DisplayName("A completed session cannot end before it started")
	void completedSession_rejectsEndBeforeStart() {
		assertThrows(IllegalArgumentException.class,
				() -> new CompletedSession(new Topic("Test"), SAFE_DATE, SAFE_DATE.minusSeconds(1)));
	}

}