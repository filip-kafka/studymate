package dev.kaffi.studymate.domain;

public class SessionAlreadyRunningException extends RuntimeException {
	public SessionAlreadyRunningException(String message) {
		super(message);
	}
	public SessionAlreadyRunningException(String message, Throwable cause) { super(message, cause);	}
}
