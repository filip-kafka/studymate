package dev.kaffi.studymate.domain;

public class StudyMateException extends RuntimeException {
    public StudyMateException(String message) {
        super(message);
    }

    public StudyMateException(String message, Throwable cause) {
        super(message, cause);
    }
}
