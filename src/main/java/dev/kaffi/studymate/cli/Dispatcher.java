package dev.kaffi.studymate.cli;

import dev.kaffi.studymate.domain.CompletedSession;
import dev.kaffi.studymate.domain.RunningSession;
import dev.kaffi.studymate.domain.SessionService;
import dev.kaffi.studymate.domain.StudyMateException;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.kaffi.studymate.cli.Outcome.*;

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
					RunningSession session = sessionService.startSession(topic);
					String message = String.format("Started session '%s' at %s", topic, formatter.formatInstant(session.start()));
					return new Result(message, SUCCESS);
				} catch (StudyMateException e) {
					return new Result(e.getMessage(), USER_ERROR);
				}
			}
			case "stop" -> {
				try {
					CompletedSession completedSession = sessionService.stopCurrentSession().get();
					String message = String.format(
							"Ended session '%s' at %s",
							completedSession.topic(),
							formatter.formatInstant(completedSession.end()));
					return new Result(message, SUCCESS);
				} catch (StudyMateException e) {
					return new Result(e.getMessage(), USER_ERROR);
				} catch (NoSuchElementException e) {
					return new Result(e.getMessage(), SYSTEM_ERROR);
				}
			}

			default -> {
				return new Result("UNRECOGNIZED COMMAND", USER_ERROR);
			}
		}
	}
}
