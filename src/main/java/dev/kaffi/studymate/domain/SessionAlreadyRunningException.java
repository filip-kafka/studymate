package dev.kaffi.studymate.domain;

public class SessionAlreadyRunningException extends StudyMateException {
	public SessionAlreadyRunningException(String message) {
		super(message);
	}
	public SessionAlreadyRunningException(String message, Throwable cause) { super(message, cause);	}
}
