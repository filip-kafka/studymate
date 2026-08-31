package dev.kaffi.studymate.storage;

import dev.kaffi.studymate.domain.Session;

import java.io.IOException;

public interface StorageManager {
	void storeSession(Session session) throws IOException;
	void deleteRunningSession() throws IOException;
}
