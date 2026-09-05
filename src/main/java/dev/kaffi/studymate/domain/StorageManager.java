package dev.kaffi.studymate.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StorageManager {
    void storeRunningSession(RunningSession session);

    void clearRunningSession();

    Optional<RunningSession> getRunningSession();

    void storeCompletedSession(CompletedSession session);

    List<CompletedSession> getCompletedSessions(Instant from, Instant toExclusive);
}
