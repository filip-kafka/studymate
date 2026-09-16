package dev.kaffi.studymate.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.ZoneId;
import dev.kaffi.studymate.cli.Formatter;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class FormatterTest {

  private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
  private static final Instant SAFE_DATE = Instant.parse("2026-03-15T09:00:00Z");

  private Formatter formatter;

  @BeforeEach
  public void setup() {
    formatter = new Formatter(PRAGUE);
  }

  @Test
  @DisplayName("Usage string is formatted correctly")
  public void FormatUsage_GivesCorrectOutput() {
    String formattedUsage = formatter.formatUsage();
    String[] expectedUsageLines = {
        "Usage:",
        "start <topic> - starts a session with given topic",
        "stop - stops and saves a running session",
        "list - lists all stored sessions"
    };
    String expectedUsage = String.join("\n", expectedUsageLines);

    assertEquals(expectedUsage, formattedUsage);
  }

  @Test
  @DisplayName("A valid session start message is formatted correctly.")
  public void FormatSessionStartMessage_formatsValidSession() {
    String topic = "JAVATEST";
    assertEquals(
        "Session JAVATEST started at 2026-03-15 10:00:00.",
        formatter.formatSessionStartMessage(topic, SAFE_DATE),
        "The session start message is not formatted correcly");
  }

  @Test
  @DisplayName("A valid session stop message is formatted correctly")
  public void FormatSessionEndMessage_formatsValidSession() {
    String topic = "JAVATEST";
    Duration duration = Duration.ofHours(2).plusMinutes(30);
    assertEquals(
      "Session JAVATEST ended after 2h:30m.",
      formatter.formatSessionEndMessage(topic, duration),
      "The session end message is not formatted correctly.");
  }

  @Test
  @DisplayName("A valid session lasting > 10 hours stop message is formatted correctly")
  public void FormatSessionEndMessage_sessionLastingMoreThan10HoursFormatsCorrectly() {
    String topic = "JAVATEST";
    Duration duration = Duration.ofHours(12).plusMinutes(30);
    assertEquals(
      "Session JAVATEST ended after 12h:30m.",
      formatter.formatSessionEndMessage(topic, duration),
      "The session end message is not formatted correctly.");
  }

  @Test
  @DisplayName("A valid session lasting > 24 hours stop message is formatted correctly")
  public void FormatSessionEndMessage_sessionLastingMoreThanADayFormatsCorrectly() {
    String topic = "JAVATEST";
    Duration duration = Duration.ofDays(3).plusHours(12);
    assertEquals(
      "Session JAVATEST ended after 84h:00m.",
      formatter.formatSessionEndMessage(topic, duration),
      "The session end message is not formatted correctly.");
  }

}
