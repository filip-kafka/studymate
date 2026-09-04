package dev.kaffi.studymate.domain;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

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

	public Optional<CompletedSession> stopCurrentSession() {
		Optional<RunningSession> runningSession = storageManager.getRunningSession();
		CompletedSession completedSession = null;
		if (runningSession.isPresent()) {
		    completedSession = runningSession.get().finish(clock.instant());
		    storageManager.storeCompletedSession(completedSession);
			storageManager.clearRunningSession();
		}
		return Optional.ofNullable(completedSession);
	}

	public Optional<RunningSession> getCurrentSession() {
		return storageManager.getRunningSession();
	}
}
