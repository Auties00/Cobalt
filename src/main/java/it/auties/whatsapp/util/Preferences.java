package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@RequiredArgsConstructor(staticName = "of")
public final class Preferences implements JacksonProvider {
    private static final Path DEFAULT_DIRECTORY;
    private static final ConcurrentHashMap<Path, ConcurrentSkipListMap<Long, CompletableFuture<Void>>> ASYNC_WRITES;

    static {
        try {
            DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsappweb4j/");
            Files.createDirectories(DEFAULT_DIRECTORY);
            ASYNC_WRITES = new ConcurrentHashMap<>();
        } catch (IOException exception) {
            throw new RuntimeException("Cannot create home path", exception);
        }
    }

    @NonNull
    private final Path file;
    private String cache;

    public static void waitAsyncOperations() {
        if (ASYNC_WRITES.isEmpty()) {
            return;
        }

        var futures = ASYNC_WRITES.values()
                .stream()
                .peek(Preferences::cancelOutdatedOperations)
                .map(ConcurrentSkipListMap::lastEntry)
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures)
                .join();
    }

    private static void cancelOutdatedOperations(ConcurrentSkipListMap<Long, CompletableFuture<Void>> data) {
        var lastEntry = data.lastEntry();
        if (lastEntry == null) {
            return;
        }

        data.headMap(lastEntry.getKey())
                .forEach((id, future) -> future.cancel(true));
    }

    public static Path home() {
        return DEFAULT_DIRECTORY;
    }

    @SneakyThrows
    public static Preferences of(String path, Object... args) {
        var location = Path.of("%s/%s".formatted(DEFAULT_DIRECTORY, path.formatted(args)));
        return new Preferences(location.toAbsolutePath());
    }

    @SneakyThrows
    public Optional<String> read() {
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        return Optional.of(Objects.requireNonNullElseGet(cache, this::readInternal));
    }

    @SneakyThrows
    public <T> T readJson(TypeReference<T> reference) {
        var json = read();
        return json.isEmpty() ?
                null :
                readAsInternal(json.get(), reference);
    }

    @SneakyThrows
    private <T> T readAsInternal(String value, TypeReference<T> reference) {
        return JSON.readValue(value, reference);
    }

    @SneakyThrows
    private String readInternal() {
        return this.cache = Files.readString(file);
    }

    public synchronized void writeJson(Object input, boolean async) {
        if (!async) {
            writeObject(input);
            return;
        }

        var writes = ASYNC_WRITES.getOrDefault(file, new ConcurrentSkipListMap<>());
        var lastEntry = writes.lastEntry();
        var id = lastEntry != null ?
                lastEntry.getKey() + 1 :
                0;
        var future = CompletableFuture.runAsync(() -> writeObject(input))
                .thenRunAsync(() -> writes.remove(id))
                .exceptionallyAsync(throwable -> onError(id, writes, throwable));
        writes.put(id, future);
        ASYNC_WRITES.put(file, writes);
    }

    private Void onError(long id, Map<Long, CompletableFuture<Void>> writes, Throwable throwable) {
        writes.remove(id);
        throwable.printStackTrace();
        return null;
    }

    private void writeObject(Object input) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(input), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot write object", exception);
        }
    }

    @SneakyThrows
    public void delete() {
        Files.deleteIfExists(file);
    }
}
