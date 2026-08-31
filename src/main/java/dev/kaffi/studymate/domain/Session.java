package dev.kaffi.studymate.domain;

import java.time.Instant;

public sealed interface Session permits RunningSession, CompletedSession {
	Topic topic();
	Instant start();
}
