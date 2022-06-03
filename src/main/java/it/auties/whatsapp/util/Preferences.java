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
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "of")
public final class Preferences implements JacksonProvider {
    private static final Path DEFAULT_DIRECTORY;

    static {
        try {
            DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsappweb4j/");
            Files.createDirectories(DEFAULT_DIRECTORY);
        }catch (IOException exception){
            throw new RuntimeException("Cannot create home path", exception);
        }
    }

    public static Path home(){
        return DEFAULT_DIRECTORY;
    }

    @NonNull
    private final Path file;

    private String cache;

    @SneakyThrows
    public static Preferences of(String path, Object... args) {
        var location = Path.of("%s/%s".formatted(DEFAULT_DIRECTORY, path.formatted(args)));
        return new Preferences(location.toAbsolutePath());
    }

    @SneakyThrows
    public Optional<String> read() {
        if(Files.notExists(file)){
            return Optional.empty();
        }

        return Optional.of(Objects.requireNonNullElseGet(cache,
                this::readInternal));
    }

    @SneakyThrows
    public <T> T readJson(TypeReference<T> reference) {
        var json = read();
        return json.isEmpty() ? null :
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

    @SneakyThrows
    public void writeJsonAsync(@NonNull Object input){
        CompletableFuture.runAsync(() -> writeJson(input));
    }

    @SneakyThrows
    public void writeJson(@NonNull Object input) {
        Files.createDirectories(file.getParent());
        Files.writeString(file, JSON.writeValueAsString(input),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @SneakyThrows
    public void delete() {
        Files.deleteIfExists(file);
    }
}
