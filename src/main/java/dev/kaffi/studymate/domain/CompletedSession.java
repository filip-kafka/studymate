package dev.kaffi.studymate.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record CompletedSession(Topic topic, Instant start, Instant end) implements Session {

    public CompletedSession {
        Objects.requireNonNull(topic, "Topic must not be null.");
        Objects.requireNonNull(start, "Start instant must not be null.");
        Objects.requireNonNull(end, "End instant must not be null.");

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Session cannot end before it started.");
        }
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
