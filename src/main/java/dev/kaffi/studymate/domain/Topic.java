package dev.kaffi.studymate.domain;

import java.util.Objects;

public record Topic(String value) {

    private static final int MAX_LENGTH = 255;

    public Topic {
        Objects.requireNonNull(value, "Topic must not be null");

        value = value.strip();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty.");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Topic length must not exceed " + MAX_LENGTH + " characters.");
        }

        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Topic must not contain ISO control characters.");
        }
    }

    @Override
    public String toString() {
        return this.value();
    }
}
