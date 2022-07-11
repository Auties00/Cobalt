package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor(staticName = "of")
public final class Preferences implements JacksonProvider {
    private static final Path DEFAULT_DIRECTORY;
    private static final Queue<CompletableFuture<Void>> ASYNC_WRITES;

    static {
        try {
            DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsappweb4j/");
            Files.createDirectories(DEFAULT_DIRECTORY);
            ASYNC_WRITES = new ConcurrentLinkedQueue<>();
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(
                            () -> CompletableFuture.allOf(ASYNC_WRITES.toArray(CompletableFuture[]::new))
                                    .join()));
        } catch (IOException exception) {
            throw new RuntimeException("Cannot create home path", exception);
        }
    }

    @NonNull
    private final Path file;

    private String cache;

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

    public void writeJson(Object input, boolean async) {
        if (!async) {
            writeObject(input);
            return;
        }

        ASYNC_WRITES.add(CompletableFuture.runAsync(() -> writeObject(input)));
    }

    @SneakyThrows
    private void writeObject(Object input) {
        Files.createDirectories(file.getParent());
        Files.writeString(file, JSON.writeValueAsString(input), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SneakyThrows
    public void delete() {
        Files.deleteIfExists(file);
    }
}
