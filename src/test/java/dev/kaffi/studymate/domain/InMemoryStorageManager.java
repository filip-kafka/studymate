package dev.kaffi.studymate.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An in-memory {@link StorageManager} for testing {@link SessionService}
 * without touching the
 * filesystem.
 *
 * <p>
 * This is a fake, not a stub: it reproduces the behaviour the service relies
 * on, including
 * rejecting a second running session the way {@code CREATE_NEW} does in the
 * file implementation.
 *
 * <p>
 * Not final, so individual tests can subclass it to simulate a failing
 * operation.
 */
class InMemoryStorageManager implements StorageManager {

    private final List<CompletedSession> completed = new ArrayList<>();
    private RunningSession running;

    @Override
    public void storeRunningSession(RunningSession session) {
        Objects.requireNonNull(session, "Session must not be null");

        if (running != null) {
            throw new SessionAlreadyRunningException("A session is already running, cannot create a new one.");
        }

        running = session;
    }

    @Override
    public void clearRunningSession() {
        running = null;
    }

    @Override
    public Optional<RunningSession> getRunningSession() {
        return Optional.ofNullable(running);
    }

    @Override
    public void storeCompletedSession(CompletedSession session) {
        Objects.requireNonNull(session, "Session must not be null");
        completed.add(session);
    }

    @Override
    public List<CompletedSession> getCompletedSessions(Instant from, Instant toExclusive) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(toExclusive, "toExclusive must not be null");

        return completed.stream()
                .filter(session -> !session.start().isBefore(from) && session.start().isBefore(toExclusive))
                .toList();
    }

    /** Test-only view of everything stored, ignoring any range. */
    List<CompletedSession> allCompletedSessions() {
        return List.copyOf(completed);
    }
}
