package it.auties.whatsapp.serialization;

import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.LocalFileSystem;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class SmileFile implements JacksonProvider {
    private static final Map<Path, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();

    @NonNull
    private final Path file;

    private SmileFile(@NonNull Path file){
        try {
            this.file = file;
            Files.createDirectories(file.getParent());
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot create preferences", exception);
        }
    }

    public static SmileFile of(@NonNull String path, @NonNull Object... args) {
        return of(LocalFileSystem.of(path.formatted(args)).toAbsolutePath());
    }

    public static SmileFile of(Path path) {
        return new SmileFile(path);
    }

    public <T> Optional<T> read(Class<T> clazz) {
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try {
            var stream = Files.newInputStream(file);
            return Optional.of(SMILE.readValue(new GZIPInputStream(stream, 65536), clazz));
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot read file", exception);
        }
    }

    public synchronized void write(Object input, boolean async) {
        var oldTask = futureMap.get(file);
        if(oldTask != null && !oldTask.isDone()){
            oldTask.cancel(true);
        }

        if (!async) {
            writeSync(input);
            return;
        }

        var newTask = CompletableFuture.runAsync(() -> writeSync(input));
        futureMap.put(file, newTask);
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
