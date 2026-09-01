package dev.kaffi.studymate.domain;

import java.time.Clock;
import java.util.Objects;

public final class SessionService {
	private final Clock clock;

	public SessionService(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
	}

	public RunningSession startSession(String topic) {
		Objects.requireNonNull(topic, "Topic must not be null");
		return new RunningSession(new Topic(topic), clock.instant());
	}

	public CompletedSession endSession(RunningSession session) {
		Objects.requireNonNull(session, "Session must not be null");
		return session.finish(clock.instant());
	}
}
