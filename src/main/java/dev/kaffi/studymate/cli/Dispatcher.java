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
            return new Result("USAGE", SUCCESS);
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                try {
                    String topic = Stream.of(args).skip(1).collect(Collectors.joining(" "));

                    if (topic.isBlank()) {
                    	throw new StudyMateException("Session topic must not be blank.");
                    }

                    RunningSession session = sessionService.startSession(topic);
                    String message = String.format("Started session '%s' at %s", session.topic().value(),
                            formatter.formatInstant(session.start()));
                    return new Result(message, SUCCESS);
                } catch (SessionAlreadyRunningException e) {
                	return new Result(e.getMessage(), USER_ERROR);
                } catch (StorageException e) {
                    return new Result(e.getMessage(), SYSTEM_ERROR);
                }
            }
            case "stop" -> {
                try {
                	Optional<CompletedSession> session = sessionService.stopCurrentSession();
                	if (session.isEmpty()) {
                		throw new StudyMateException("No session is currently running.");
                	}
                    CompletedSession completedSession = session.get();
                    String message = String.format(
                            "Ended session '%s' at %s",
                            completedSession.topic().value(),
                            formatter.formatInstant(completedSession.end()));
                    return new Result(message, SUCCESS);
                } catch (StudyMateException e) {
                    return new Result(e.getMessage(), USER_ERROR);
                }
            }

            default -> {
                return new Result("Usage:\nstart <topic> - starts a session with given topic\nstop - stops and saves a running session", USER_ERROR);
            }
        }
    }
}
