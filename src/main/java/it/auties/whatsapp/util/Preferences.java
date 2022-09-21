package it.auties.whatsapp.util;

import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class Preferences implements JacksonProvider {
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsappweb4j/");
    static {
        try {
            Files.createDirectories(DEFAULT_DIRECTORY);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot create home directory", exception);
        }
    }

    @NonNull
    private final Path file;

    private Preferences(@NonNull Path file){
        try {
            this.file = file;
            Files.createDirectories(file.getParent());
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot create preferences", exception);
        }
    }

    public static Preferences of(String path, Object... args) {
        var location = Path.of("%s/%s".formatted(DEFAULT_DIRECTORY, path.formatted(args)));
        return new Preferences(location.toAbsolutePath());
    }

    public static Path home() {
        return DEFAULT_DIRECTORY;
    }

    public <T> T read(Class<T> clazz) {
        if (Files.notExists(file)) {
            return null;
        }

        try {
            var stream = Files.newInputStream(file);
            return SMILE.readValue(new GZIPInputStream(stream), clazz);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read file", exception);
        }
    }

    public synchronized void write(Object input, boolean async) {
        if (!async) {
            writeSync(input);
            return;
        }

        CompletableFuture.runAsync(() -> writeSync(input));
    }

    private void writeSync(Object input) {
        try {
            var stream = Files.newOutputStream(file, StandardOpenOption.CREATE);
            SMILE.writeValue(new GZIPOutputStream(stream), input);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot write object", exception);
        }
    }
}
