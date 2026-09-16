package dev.kaffi.studymate.cli;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Formatter {

  private static final String[] USAGE_LINES = {
    "Usage:",
    "start <topic> - starts a session with given topic",
    "stop - stops and saves a running session",
    "list - lists all stored sessions"
  };

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ZoneId zoneId;

  public Formatter(ZoneId zoneId) {
      this.zoneId = Objects.requireNonNull(zoneId, "ZoneId must not be null");
  }

  public String formatSessionStartMessage(String topic, Instant start) {
    return String.format("Session %s started at %s.", topic, formatInstant(start));
  }

  public String formatSessionEndMessage(String topic, Duration duration) {
    return String.format("Session %s ended after %s.", topic, formatDuration(duration));
  }

  public String formatUsage() {
    return Stream.of(USAGE_LINES).collect(Collectors.joining("\n")).toString();
  }

  private String formatInstant(Instant instant) {
      ZonedDateTime zonedDateTime = instant.atZone(zoneId);
      return DATE_TIME_FORMATTER.format(zonedDateTime);
  }

  private String formatDuration(Duration duration) {
      long hours = duration.toHours();
      long minutes = duration.toMinutes() % 60;
      return String.format("%dh:%02dm", hours, minutes);
  }

}
