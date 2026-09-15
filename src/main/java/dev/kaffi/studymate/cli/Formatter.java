package dev.kaffi.studymate.cli;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Formatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final ZoneId zoneId;

    public Formatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "ZoneId must not be null");
    }

    public String formatInstant(Instant instant) {
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        return DATE_TIME_FORMATTER.format(zonedDateTime);
    }

    public String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%02dh:02%dm", hours, minutes);
    }
}
