package dev.kaffi.studymate;

import dev.kaffi.studymate.cli.Dispatcher;
import dev.kaffi.studymate.cli.Formatter;
import dev.kaffi.studymate.cli.Result;
import dev.kaffi.studymate.domain.SessionService;
import dev.kaffi.studymate.storage.FileStorageManager;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

public class Main {

	final static Formatter formatter = new Formatter();
	final static SessionService sessionService = new SessionService(
			Clock.tick(Clock.systemUTC(), Duration.ofSeconds(1)),
			new FileStorageManager(Path.of(System.getProperty("user.home")).resolve(".studymate"))
	);
	final static Dispatcher dispatcher = new Dispatcher(sessionService, formatter);

	static void main(String[] args) {
		Result dispatchResult = dispatcher.dispatch(args);

		switch (dispatchResult.outcome()) {
			case SUCCESS -> {
				System.out.println(dispatchResult.content());
				System.exit(0);
			}
			case USER_ERROR -> {
				System.err.println(dispatchResult.content());
				System.exit(1);
			}
			case SYSTEM_ERROR -> {
				System.err.println(dispatchResult.content());
				System.exit(2);
			}
		}
	}
}
