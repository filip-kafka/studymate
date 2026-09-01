package dev.kaffi.studymate.storage;

import dev.kaffi.studymate.domain.CompletedSession;
import dev.kaffi.studymate.domain.RunningSession;
import dev.kaffi.studymate.domain.SessionAlreadyRunningException;
import dev.kaffi.studymate.domain.StorageException;
import dev.kaffi.studymate.domain.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageManagerTest {

	private static final Instant SAFE_DATE = Instant.parse("2026-03-15T09:00:00Z");
	private static final Topic TOPIC = new Topic("Java Generics");

	private static final String RUNNING_FILE = "running.txt";
	private static final String COMPLETED_FILE = "store.tsv";

	@TempDir
	private Path tempDir;

	private Path baseDir;
	private FileStorageManager manager;

	@BeforeEach
	void setup() {
		baseDir = tempDir.resolve("store");
		manager = new FileStorageManager(baseDir);
	}

	// ====================
	// construction
	// ====================

	@Test
	@DisplayName("The base directory is created when it does not exist")
	void constructor_createsMissingBaseDirectory() {
		assertTrue(Files.isDirectory(baseDir));
	}

	@Test
	@DisplayName("Missing parent directories are created too")
	void constructor_createsMissingParentDirectories() {
		Path nested = tempDir.resolve("a/b/c");

		new FileStorageManager(nested);

		assertTrue(Files.isDirectory(nested));
	}

	@Test
	@DisplayName("An existing base directory is accepted")
	void constructor_acceptsExistingBaseDirectory() {
		assertDoesNotThrow(() -> new FileStorageManager(baseDir));
	}

	@Test
	@DisplayName("Re-opening a base directory keeps the data already stored there")
	void constructor_preservesExistingData() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		FileStorageManager reopened = new FileStorageManager(baseDir);

		assertEquals(Optional.of(new RunningSession(TOPIC, SAFE_DATE)), reopened.getRunningSession());
	}

	@Test
	@DisplayName("A base directory that cannot be created is reported as a storage failure")
	void constructor_wrapsDirectoryCreationFailure() throws IOException {
		Path file = Files.createFile(tempDir.resolve("not-a-directory"));

		assertThrows(StorageException.class, () -> new FileStorageManager(file.resolve("store")));
	}

	@Test
	@DisplayName("A null base directory is rejected")
	void constructor_rejectsNullBaseDir() {
		assertThrows(NullPointerException.class, () -> new FileStorageManager(null));
	}

	// ====================
	// storeRunningSession
	// ====================

	@Test
	@DisplayName("Storing a running session writes the running session file")
	void storeRunningSession_writesRunningFile() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		assertTrue(Files.isRegularFile(baseDir.resolve(RUNNING_FILE)));
	}

	@Test
	@DisplayName("The running session is written as a single tab separated line")
	void storeRunningSession_writesTopicAndStartSeparatedByTab() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		assertEquals("Java Generics\t2026-03-15T09:00:00Z\n", read(RUNNING_FILE));
	}

	@Test
	@DisplayName("Storing a running session leaves the completed session store untouched")
	void storeRunningSession_doesNotTouchCompletedStore() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		assertFalse(Files.exists(baseDir.resolve(COMPLETED_FILE)));
	}

	@Test
	@DisplayName("Starting a second session while one is running is rejected")
	void storeRunningSession_rejectsSecondConcurrentSession() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		assertThrows(SessionAlreadyRunningException.class,
				() -> manager.storeRunningSession(new RunningSession(new Topic("Java Streams"), SAFE_DATE)));
	}

	@Test
	@DisplayName("A rejected second session does not overwrite the running one")
	void storeRunningSession_keepsFirstSessionAfterRejection() {
		RunningSession first = new RunningSession(TOPIC, SAFE_DATE);
		manager.storeRunningSession(first);

		assertThrows(SessionAlreadyRunningException.class,
				() -> manager.storeRunningSession(new RunningSession(new Topic("Java Streams"), SAFE_DATE.plusSeconds(60))));

		assertEquals(Optional.of(first), manager.getRunningSession());
	}

	@Test
	@DisplayName("A session can be started again once the previous one was cleared")
	void storeRunningSession_allowsNewSessionAfterClear() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));
		manager.clearRunningSession();

		RunningSession next = new RunningSession(new Topic("Java Streams"), SAFE_DATE.plusSeconds(60));
		manager.storeRunningSession(next);

		assertEquals(Optional.of(next), manager.getRunningSession());
	}

	@Test
	@DisplayName("A write failure is reported as a storage failure")
	void storeRunningSession_wrapsWriteFailure() throws IOException {
		Files.delete(baseDir);

		assertThrows(StorageException.class, () -> manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE)));
	}

	@Test
	@DisplayName("A null running session is rejected")
	void storeRunningSession_rejectsNullSession() {
		assertThrows(NullPointerException.class, () -> manager.storeRunningSession(null));
	}

	// ====================
	// clearRunningSession
	// ====================

	@Test
	@DisplayName("Clearing removes the running session file")
	void clearRunningSession_removesRunningFile() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		manager.clearRunningSession();

		assertFalse(Files.exists(baseDir.resolve(RUNNING_FILE)));
	}

	@Test
	@DisplayName("Clearing leaves no running session behind")
	void clearRunningSession_leavesNoRunningSession() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		manager.clearRunningSession();

		assertEquals(Optional.empty(), manager.getRunningSession());
	}

	@Test
	@DisplayName("Clearing when nothing is running does not throw")
	void clearRunningSession_doesNotThrowWhenNothingRunning() {
		assertDoesNotThrow(() -> manager.clearRunningSession());
	}

	@Test
	@DisplayName("Clearing twice does not throw")
	void clearRunningSession_isIdempotent() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));
		manager.clearRunningSession();

		assertDoesNotThrow(() -> manager.clearRunningSession());
	}

	@Test
	@DisplayName("Clearing leaves the completed session store untouched")
	void clearRunningSession_doesNotTouchCompletedStore() {
		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plusSeconds(60)));
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		manager.clearRunningSession();

		assertEquals("Java Generics\t2026-03-15T09:00:00Z\t2026-03-15T09:01:00Z\n", read(COMPLETED_FILE));
	}

	// ====================
	// getRunningSession
	// ====================

	@Test
	@DisplayName("No running session is reported when the store is empty")
	void getRunningSession_returnsEmptyWhenNothingStored() {
		assertEquals(Optional.empty(), manager.getRunningSession());
	}

	@Test
	@DisplayName("A stored running session is read back unchanged")
	void getRunningSession_roundTripsStoredSession() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		Optional<RunningSession> running = manager.getRunningSession();

		assertAll(
				() -> assertTrue(running.isPresent()),
				() -> assertEquals(TOPIC, running.orElseThrow().topic()),
				() -> assertEquals(SAFE_DATE, running.orElseThrow().start())
		);
	}

	@Test
	@DisplayName("Sub-second precision of the start instant survives a round trip")
	void getRunningSession_preservesNanosecondPrecision() {
		Instant precise = Instant.parse("2026-03-15T09:00:00.123456789Z");
		manager.storeRunningSession(new RunningSession(TOPIC, precise));

		assertEquals(precise, manager.getRunningSession().orElseThrow().start());
	}

	@Test
	@DisplayName("A topic containing spaces survives a round trip")
	void getRunningSession_preservesTopicWithSpaces() {
		Topic spaced = new Topic("Java  Generics and Streams");
		manager.storeRunningSession(new RunningSession(spaced, SAFE_DATE));

		assertEquals(spaced, manager.getRunningSession().orElseThrow().topic());
	}

	@Test
	@DisplayName("A non-ASCII topic survives a round trip")
	void getRunningSession_preservesNonAsciiTopic() {
		Topic accented = new Topic("Řetězce v Javě");
		manager.storeRunningSession(new RunningSession(accented, SAFE_DATE));

		assertEquals(accented, manager.getRunningSession().orElseThrow().topic());
	}

	@ParameterizedTest(name = "content = \"{0}\"")
	@ValueSource(strings = {
			"",
			"\n",
			"Java Generics\n",
			"Java Generics\t2026-03-15T09:00:00Z\tJava Streams\n"
	})
	@DisplayName("A running session file with the wrong number of fields is reported as corrupted")
	void getRunningSession_rejectsWrongFieldCount(String content) {
		write(RUNNING_FILE, content);

		assertThrows(StorageException.class, () -> manager.getRunningSession());
	}

	@ParameterizedTest(name = "start = \"{0}\"")
	@ValueSource(strings = {"not-an-instant", "2026-03-15", "2026-13-15T09:00:00Z", " ", "Java Generics\t\n"})
	@DisplayName("A running session file with an unparseable start instant is reported as corrupted")
	void getRunningSession_rejectsUnparseableStart(String start) {
		write(RUNNING_FILE, "Java Generics\t" + start + "\n");

		assertThrows(StorageException.class, () -> manager.getRunningSession());
	}

	@Test
	@DisplayName("A running session file with an invalid topic is reported as corrupted")
	void getRunningSession_rejectsInvalidTopic() {
		write(RUNNING_FILE, "\t2026-03-15T09:00:00Z\n");

		assertThrows(StorageException.class, () -> manager.getRunningSession());
	}

	@Test
	@DisplayName("A read failure is reported as a storage failure")
	void getRunningSession_wrapsReadFailure() throws IOException {
		Files.createDirectory(baseDir.resolve(RUNNING_FILE));

		assertThrows(StorageException.class, () -> manager.getRunningSession());
	}

	// ====================
	// storeCompletedSession
	// ====================

	@Test
	@DisplayName("Storing a completed session writes the completed session store")
	void storeCompletedSession_writesCompletedStore() {
		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plus(Duration.ofHours(2))));

		assertTrue(Files.isRegularFile(baseDir.resolve(COMPLETED_FILE)));
	}

	@Test
	@DisplayName("The completed session is written as a single tab separated line")
	void storeCompletedSession_writesTopicStartAndEndSeparatedByTabs() {
		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plus(Duration.ofHours(2))));

		assertEquals("Java Generics\t2026-03-15T09:00:00Z\t2026-03-15T11:00:00Z\n", read(COMPLETED_FILE));
	}

	@Test
	@DisplayName("Completed sessions are appended in the order they were stored")
	void storeCompletedSession_appendsInInsertionOrder() {
		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plusSeconds(60)));
		manager.storeCompletedSession(new CompletedSession(new Topic("Java Streams"), SAFE_DATE.plusSeconds(120), SAFE_DATE.plusSeconds(180)));

		assertEquals("""
						Java Generics\t2026-03-15T09:00:00Z\t2026-03-15T09:01:00Z
						Java Streams\t2026-03-15T09:02:00Z\t2026-03-15T09:03:00Z
						""",
				read(COMPLETED_FILE));
	}

	@Test
	@DisplayName("Completed sessions stored by a later instance are appended, not overwritten")
	void storeCompletedSession_appendsAcrossInstances() {
		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plusSeconds(60)));

		new FileStorageManager(baseDir)
				.storeCompletedSession(new CompletedSession(new Topic("Java Streams"), SAFE_DATE.plusSeconds(120), SAFE_DATE.plusSeconds(180)));

		assertEquals(2, read(COMPLETED_FILE).lines().count());
	}

	@Test
	@DisplayName("A zero length completed session is stored")
	void storeCompletedSession_storesZeroLengthSession() {
		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE));

		assertEquals("Java Generics\t2026-03-15T09:00:00Z\t2026-03-15T09:00:00Z\n", read(COMPLETED_FILE));
	}

	@Test
	@DisplayName("Storing a completed session leaves the running session untouched")
	void storeCompletedSession_doesNotTouchRunningSession() {
		manager.storeRunningSession(new RunningSession(TOPIC, SAFE_DATE));

		manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plusSeconds(60)));

		assertEquals(Optional.of(new RunningSession(TOPIC, SAFE_DATE)), manager.getRunningSession());
	}

	@Test
	@DisplayName("A write failure is reported as a storage failure")
	void storeCompletedSession_wrapsWriteFailure() throws IOException {
		Files.delete(baseDir);

		assertThrows(StorageException.class,
				() -> manager.storeCompletedSession(new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plusSeconds(60))));
	}

	@Test
	@DisplayName("A null completed session is rejected")
	void storeCompletedSession_rejectsNullSession() {
		assertThrows(NullPointerException.class, () -> manager.storeCompletedSession(null));
	}

	// ====================
	// getCompletedSessions
	// ====================

	@Test
	@DisplayName("No completed sessions are reported when the store is empty")
	void getCompletedSessions_returnsEmptyWhenNothingStored() {
		assertEquals(List.of(), manager.getCompletedSessions(SAFE_DATE, SAFE_DATE.plus(Duration.ofDays(1))));
	}

	@Test
	@Disabled("getCompletedSessions is not implemented yet - it currently always returns an empty list")
	@DisplayName("Completed sessions are selected by start instant in the range <from, toExclusive)")
	void getCompletedSessions_selectsByStartInstantInHalfOpenRange() {
		CompletedSession before = new CompletedSession(TOPIC, SAFE_DATE.minusSeconds(1), SAFE_DATE);
		CompletedSession atFrom = new CompletedSession(TOPIC, SAFE_DATE, SAFE_DATE.plusSeconds(60));
		CompletedSession inside = new CompletedSession(TOPIC, SAFE_DATE.plusSeconds(30), SAFE_DATE.plusSeconds(90));
		CompletedSession atTo = new CompletedSession(TOPIC, SAFE_DATE.plusSeconds(60), SAFE_DATE.plusSeconds(120));

		List.of(before, atFrom, inside, atTo).forEach(manager::storeCompletedSession);

		assertEquals(List.of(atFrom, inside),
				manager.getCompletedSessions(SAFE_DATE, SAFE_DATE.plusSeconds(60)));
	}

	// ====================
	// helpers
	// ====================

	private String read(String fileName) {
		try {
			return Files.readString(baseDir.resolve(fileName), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("Could not read " + fileName, e);
		}
	}

	private void write(String fileName, String content) {
		try {
			Files.writeString(baseDir.resolve(fileName), content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("Could not write " + fileName, e);
		}
	}
}
