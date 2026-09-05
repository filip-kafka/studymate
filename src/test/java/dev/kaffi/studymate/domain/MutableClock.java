package dev.kaffi.studymate.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * A {@link Clock} whose current instant can be moved forward on demand.
 */
public final class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(Instant start, ZoneId zone) {
        this.instant = Objects.requireNonNull(start, "start");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        Objects.requireNonNull(newZone, "newZone");
        if (newZone.equals(this.zone)) {
            return this;
        }
        return new MutableClock(instant, newZone);
    }

    /** Moves the clock forward by the given amount. */
    public void advance(Duration amount) {
        Objects.requireNonNull(amount, "amount");
        this.instant = this.instant.plus(amount);
    }

    /**
     * Jumps the clock to an arbitrary instant for tests that need a specific
     * calendar date.
     */
    public void setTo(Instant newInstant) {
        this.instant = Objects.requireNonNull(newInstant, "newInstant");
    }
}
