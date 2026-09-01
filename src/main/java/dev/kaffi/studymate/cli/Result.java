package dev.kaffi.studymate.cli;

import java.util.Objects;

public record Result(String content, Outcome outcome) {
	public Result {
		Objects.requireNonNull(content, "Content must not be null");
		Objects.requireNonNull(outcome, "Outcome must not be null");
	}
}
