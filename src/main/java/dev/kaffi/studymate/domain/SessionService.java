package dev.kaffi.studymate.domain;

import java.time.Clock;
import java.util.Objects;

public final class SessionService {
	private final Clock clock;
	private final StorageManager storageManager;

	public SessionService(Clock clock, StorageManager storageManager) {
		this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
		this.storageManager = Objects.requireNonNull(storageManager, "Storage manager must not be null");
	}

	public RunningSession startSession(String topic) {
		Objects.requireNonNull(topic, "Topic must not be null");

		if (storageManager.getRunningSession().isPresent()) {
			throw new SessionAlreadyRunningException("A session is already running. Finish the current session before starting a new one.");
		}

		RunningSession session = new RunningSession(new Topic(topic), clock.instant());
		storageManager.storeRunningSession(session);
		return session;
	}

	public CompletedSession endSession(RunningSession session) {
		Objects.requireNonNull(session, "Session must not be null");

		CompletedSession completedSession = session.finish(clock.instant());
		storageManager.storeCompletedSession(completedSession);
		storageManager.clearRunningSession();
		return completedSession;
	}
}
