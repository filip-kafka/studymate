package dev.kaffi.studymate.cli;

import static dev.kaffi.studymate.cli.Outcome.SUCCESS;
import static dev.kaffi.studymate.cli.Outcome.SYSTEM_ERROR;
import static dev.kaffi.studymate.cli.Outcome.USER_ERROR;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.kaffi.studymate.domain.CompletedSession;
import dev.kaffi.studymate.domain.RunningSession;
import dev.kaffi.studymate.domain.SessionAlreadyRunningException;
import dev.kaffi.studymate.domain.SessionService;
import dev.kaffi.studymate.domain.StorageException;
import dev.kaffi.studymate.domain.StudyMateException;

public class Dispatcher {

  private final SessionService sessionService;
  private final Formatter formatter;

  public Dispatcher(SessionService sessionService, Formatter formatter) {
    this.sessionService = Objects.requireNonNull(sessionService, "Session service must not be null");
    this.formatter = Objects.requireNonNull(formatter, "Formatter must not be null");
  }

  public Result dispatch(String[] args) {

    if (args.length == 0) {
      return new Result(formatter.formatUsage(), SUCCESS);
    }

    switch (args[0].toLowerCase()) {
      case "start" -> {
        try {
          String topic = Stream.of(args).skip(1).collect(Collectors.joining(" "));

          if (topic.isBlank()) {
            return new Result("Topic must not be blank.", USER_ERROR);
          }

          RunningSession session = sessionService.startSession(topic);

          return new Result(formatter.formatSessionStartMessage(session.topic().value(), session.start()), SUCCESS);

        } catch (SessionAlreadyRunningException e) {
          return new Result(e.getMessage(), USER_ERROR);
        } catch (StudyMateException e) {
          return new Result(e.getMessage(), SYSTEM_ERROR);
        }
      }
      case "stop" -> {
        try {
          Optional<CompletedSession> session = sessionService.stopCurrentSession();

          if (session.isEmpty()) {
            return new Result("No session is running.", USER_ERROR);
          }

          CompletedSession completedSession = session.get();

          return new Result(
              formatter.formatSessionEndMessage(completedSession.topic().value(), completedSession.duration()),
              SUCCESS);

        } catch (StorageException e) {
          return new Result(e.getMessage(), SYSTEM_ERROR);
        } catch (StudyMateException e) {
          return new Result(e.getMessage(), USER_ERROR);
        }
      }

      default -> {
        return new Result(formatter.formatUsage(), SUCCESS);
      }
    }
  }
}
