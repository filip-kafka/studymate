package dev.kaffi.studymate.storage;

import dev.kaffi.studymate.domain.CompletedSession;
import dev.kaffi.studymate.domain.RunningSession;
import dev.kaffi.studymate.domain.Session;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileStorageManager implements StorageManager {

	public final Path BASE_STORAGE_PATH = Path.of(System.getProperty("user.home"), ".studymate");

	public void storeSession(Session session) throws IOException {
		if (!Files.exists(BASE_STORAGE_PATH)) {
			Files.createDirectories(BASE_STORAGE_PATH);
		}
		switch (session) {
			case null -> { return; }
			case RunningSession s -> storeRunningSession(s);
			case CompletedSession s -> storeCompletedSession(s);
		};
	}

	public void deleteRunningSession() throws IOException {
		Files.deleteIfExists(Path.of(BASE_STORAGE_PATH.toString(), "/running.txt"));
	}

	private void storeRunningSession(RunningSession s) {
		File file = new File(Path.of(BASE_STORAGE_PATH.toString(), "/running.txt").toString());

		if (file.exists()) {
			throw new IllegalStateException("A session is already running, it is not possible to start another one.");
		}

		try (FileWriter fw = new FileWriter(file)) {
			fw.write(String.format("%s\t%s", s.topic(), s.start().toString()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void storeCompletedSession(CompletedSession s) {
		File file = new File(Path.of(BASE_STORAGE_PATH.toString(), "/store.tsv").toString());
		try (FileWriter fw = new FileWriter(file, true)) {
			fw.append(String.format("%s\t%s\t%s\n", s.topic(), s.start().toString(), s.end().toString()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
