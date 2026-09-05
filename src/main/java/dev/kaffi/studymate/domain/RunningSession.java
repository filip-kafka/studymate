package dev.kaffi.studymate.domain;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;

public record RunningSession(Topic topic, Instant start) implements Session {

	public RunningSession {
		Objects.requireNonNull(topic, "Topic must not be null.");
		Objects.requireNonNull(start, "Start instant must not be null.");
	}

	public Duration elapsed(Instant now) {
	    return Duration.between(this.start(), now);
	}

	public CompletedSession finish(Instant end) {
		return new CompletedSession(topic, start, end);
	}
}
