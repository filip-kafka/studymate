package dev.kaffi.studymate.domain;

public class StorageException extends StudyMateException {
	public StorageException(String message) {
		super(message);
	}
	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
