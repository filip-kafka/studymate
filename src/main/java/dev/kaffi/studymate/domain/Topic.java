package dev.kaffi.studymate.domain;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record Topic(String value) {
	public Topic {
		Objects.requireNonNull(value, "Topic must not be null");

		value = value.strip();

		if (value.isEmpty()) {
			throw new IllegalArgumentException("Topic cannot be empty.");
		}

		if (value.length() > 255) {
			throw new IllegalArgumentException("Topic length must not exceed 255 characters.");
		}

		if (value.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("Topic must not contain ISO control characters.");
		}
	}

	@Override
	@NonNull
	public String toString() {
		return this.value();
	}
}
