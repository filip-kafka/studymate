![build](https://github.com/filip-kafka/studymate/actions/workflows/build.yml/badge.svg)

# StudyMate

A command-line study session tracker, written in Java as a learning project.

The idea is pretty simple: start a timer for a topic, stop it when you're done, and keep track of how long you actually studied. Later plans include a short reflection after each session and some basic reporting.

## Status

**In development — there is no runnable application yet.**

## Building

Requires JDK 25 and Maven.

```text
mvn verify
```

This compiles the project and runs the full test suite. There isn't really anything useful to run yet.

## Design decisions - mostly just for me to remember why

These are the choices I've made so far and, more importantly, why I made them. Some of them are probably more elaborate than they need to be for a small study timer, but this is primarily a project for learning.

### Timestamps are stored as `Instant`, not `LocalDateTime`

A study session happened at a specific point in time, so `Instant` seemed like the better fit. `LocalDateTime` is basically a date and time on a wall clock and doesn't tell you which point on the timeline it actually represents by itself.

This also means a session can cross a DST transition without the duration calculation doing anything weird. And yes, I occasionally study at 2/3 AM.

`SessionServiceTest.endSession_durationIsUnaffectedByDstTransition` covers this as a regression test.

### The clock is injected, not called

`SessionService` takes a `java.time.Clock` in its constructor instead of calling `Instant.now()` directly. The real clock gets created once and passed in.

The main reason is testing. Otherwise testing something involving the passage of time gets annoying very quickly. With an injected clock I can just move time forward in a test instead of actually waiting.

It also makes the DST test possible whenever I want, rather than twice a year.

### Running and completed sessions are separate types

`Session` is a sealed interface with exactly two implementations: `RunningSession` (topic, start) and `CompletedSession` (topic, start, end).

Stopping a running session returns a completed one.

I considered having one `Session` with a nullable `end` value, but then every piece of code dealing with sessions has to remember that the session might not be finished yet. With two types, a `CompletedSession` simply can't exist without an end time.

The sealed interface also means a `switch` over `Session` can be exhaustive, so the compiler complains if I add another session type and forget to handle it somewhere.

### Duration is derived, not stored

`CompletedSession.duration()` calculates the duration from `start` and `end`.

I don't see much value in storing a third value that can disagree with the other two. One source of truth seems simpler.

### `Topic` is a value type, not a `String`

A topic has some validation and normalization rules, so I put those in `Topic` rather than spreading them around the services.

This is also important because the storage layer creates domain objects directly when loading data. It doesn't go through `SessionService`, so validation that only lives in the service could otherwise be bypassed.

### Storage is an interface in the domain, implemented outside it

`StorageManager` lives in `domain`, while the actual implementations live in `storage`.

The domain knows what it needs from storage, but doesn't need to know how the data is actually stored.

Deliberately not sealed because I expect there will be more implementations later.

### Completed sessions are written before the running marker is cleared

Ending a session involves two writes, and they aren't atomic.

If the application crashes between the two operations, I might end up with a duplicate when recovering, but I'd rather deal with a duplicate than lose the completed session entirely.

### The file format is plain text, not something compact

There are currently two files under a base directory supplied when the storage manager is created:

* `running.txt` — one tab-separated line, present only while a session is running
* `store.tsv` — append-only, one completed session per line

Instants are stored as ISO-8601 strings.

This is not going to be a high-volume application, so I don't care much about saving a few bytes. Being able to open the files and immediately understand what's in them is more useful to me.

The existence of `running.txt` is also the answer to "is a session running?" 
It is created with `CREATE_NEW`, so starting another session fails atomically if the file already exists.

Topics are rejected if they contain ISO control characters.

### Storage failures use one exception type

Everything crossing the storage boundary is wrapped in `StorageException`, including parsing failures.

The idea is that callers shouldn't need to know whether something failed because of an I/O problem, a malformed line in the file, etc.

`SessionAlreadyRunningException` is different because that's a domain rule rather than a storage problem. It is therefore a sibling of `StorageException`, and both extend `StudyMateException`.

## Testing

The domain tests don't touch the filesystem or the real clock.

There are two small test implementations for this:

* `MutableClock` — a `Clock` whose instant I can move forward in tests.
* `InMemoryStorageManager` — behaves enough like the real storage manager for the service tests without actually writing files.

`FileStorageManagerTest` is the only test class that does real I/O. It uses JUnit's `@TempDir`, so the tests don't touch any real data.

## Roadmap

**v1** — make it actually usable

* Log study sessions
* Read completed sessions back from the store
* CLI: `start <topic>`, `stop`, `status`, `today`
* Human-readable duration formatting
* Executable jar and a shell alias

**v2**

* Post-session reflection: focus, distraction, and session quality on a scale
* Querying and filtering past sessions
* SQLite backend behind the same `StorageManager` interface
* Pattern analysis: which hours, days, and environments seem to correlate with better focus and study quality
