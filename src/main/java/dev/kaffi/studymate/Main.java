package dev.kaffi.studymate;

import dev.kaffi.studymate.cli.Dispatcher;
import dev.kaffi.studymate.cli.Formatter;
import dev.kaffi.studymate.cli.Result;
import dev.kaffi.studymate.domain.SessionService;
import dev.kaffi.studymate.domain.StorageManager;
import dev.kaffi.studymate.domain.StudyMateException;
import dev.kaffi.studymate.storage.FileStorageManager;
import jdk.internal.org.commonmark.internal.inline.AsteriskDelimiterProcessor;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

public class Main {

	public static void main(String[] args) {

	    String baseDir = System.getenv("STUDYMATE_HOME");
	    Path path = Path.of(baseDir != null ? baseDir : System.getProperty("user.home")).resolve(".studymate");

	    StorageManager storageManager = new FileStorageManager(path);

	    Formatter formatter = new Formatter();
		SessionService sessionService = new SessionService(Clock.tick(Clock.systemDefaultZone(), Duration.ofSeconds(1)), storageManager);
		Dispatcher dispatcher = new Dispatcher(sessionService, formatter);

		try {
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
		} catch (StudyMateException e) {
			System.err.println(e.getMessage());
			System.exit(2);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(2);
		}
	}
}
