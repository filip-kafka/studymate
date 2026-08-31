package dev.kaffi.studymate.domain;

import dev.kaffi.studymate.storage.StorageManager;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;

public final class SessionService {
	private final Clock clock;

	public SessionService(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
	}

	public RunningSession startSession(String topic, StorageManager storageManager) throws IOException {
		RunningSession session = new RunningSession(new Topic(topic), clock.instant());
		storageManager.storeSession(session);
		return session;
	}

	public CompletedSession endSession(RunningSession session, StorageManager storageManager) throws IOException {
		CompletedSession completedSession = session.finish(clock.instant());
		storageManager.deleteRunningSession();
		storageManager.storeSession(completedSession);
		return completedSession;
	}
}
