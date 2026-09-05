package dev.kaffi.studymate.storage;

import dev.kaffi.studymate.domain.CompletedSession;
import dev.kaffi.studymate.domain.RunningSession;
import dev.kaffi.studymate.domain.SessionAlreadyRunningException;
import dev.kaffi.studymate.domain.StorageException;
import dev.kaffi.studymate.domain.StorageManager;
import dev.kaffi.studymate.domain.Topic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Optional;

public final class FileStorageManager implements StorageManager {

    private final Path runningStore;
    private final Path completedStore;

    public FileStorageManager(Path baseDir) {
        Objects.requireNonNull(baseDir, "Base directory path must not be null");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new StorageException("Could not create base directory.", e);
        }

        this.runningStore = baseDir.resolve("running.txt");
        this.completedStore = baseDir.resolve("store.tsv");
    }

    @Override
    public void storeRunningSession(RunningSession session) {
        Objects.requireNonNull(session, "Session must not be null");
        try {
            Files.writeString(runningStore, String.format("%s\t%s\n", session.topic().value(), session.start()),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            throw new SessionAlreadyRunningException("A session is already running, cannot create a new one.", e);
        } catch (IOException e) {
            throw new StorageException("Could not write running session.", e);
        }
    }

    @Override
    public void clearRunningSession() {
        try {
            Files.deleteIfExists(runningStore);
        } catch (IOException e) {
            throw new StorageException("Could not delete running session file.", e);
        }
    }

    @Override
    public Optional<RunningSession> getRunningSession() {
        try {
            String[] fields = Files.readString(runningStore, StandardCharsets.UTF_8).lines().findFirst().orElse("")
                    .split("\t", -1);
            if (fields.length != 2) {
                throw new StorageException("The running session file [" + runningStore + "] is corrupted.");
            }
            return Optional.of(new RunningSession(new Topic(fields[0]), Instant.parse(fields[1])));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new StorageException("Failed to recreate the RunningSession", e);
        } catch (IOException e) {
            throw new StorageException("Failed to read file " + runningStore, e);
        }
    }

    @Override
    public void storeCompletedSession(CompletedSession session) {
        Objects.requireNonNull(session, "Session must not be null");
        try {
            Files.writeString(completedStore,
                    String.format("%s\t%s\t%s\n", session.topic().value(), session.start(), session.end()),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new StorageException("Could not write completed session.", e);
        }
    }

    @Override
    public List<CompletedSession> getCompletedSessions(Instant from, Instant toExclusive) {
        List<CompletedSession> sessions = loadCompletedSessions();

        return List.copyOf(sessions.stream()
                .filter(session -> (!session.start().isBefore(from) && session.start().isBefore(toExclusive)))
                .collect(Collectors.toList()));
    }

    private List<CompletedSession> loadCompletedSessions() {
        List<CompletedSession> result = new ArrayList<>();
        try {
            List<String> sessions = Files.readAllLines(completedStore);
            for (String session : sessions) {
                String[] fields = session.split("\t", -1);
                if (fields.length != 3) {
                    continue;
                }
                Topic topic = new Topic(fields[0]);
                Instant start = Instant.parse(fields[1]);
                Instant end = Instant.parse(fields[2]);
                CompletedSession current = new CompletedSession(topic, start, end);
                result.add(current);
            }
        } catch (IOException e) {
            return List.of();
        }
        return result;
    }
}
