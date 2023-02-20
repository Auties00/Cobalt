package it.auties.whatsapp.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.LocalFileSystem;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class SmileFile implements JacksonProvider {
    @NonNull
    private final Path file;

    private SmileFile(@NonNull Path file) {
        try {
            this.file = file;
            Files.createDirectories(file.getParent());
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create smile file", exception);
        }
    }

    public static SmileFile of(@NonNull String path, @NonNull Object... args) {
        return of(LocalFileSystem.of(path.formatted(args)).toAbsolutePath());
    }

    public static SmileFile of(Path path) {
        return new SmileFile(path);
    }

    public <T> Optional<T> read(Class<T> clazz) throws IOException {
        return read(new TypeReference<>() {
            @Override
            public Class<T> getType() {
                return clazz;
            }
        });
    }

    public <T> Optional<T> read(TypeReference<T> reference) throws IOException {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        var stream = Files.newInputStream(file);
        return Optional.of(SMILE.readValue(new GZIPInputStream(stream), reference));
    }

    public void write(Object input, boolean async) {
        if (!async) {
            writeSync(input);
            CompletableFuture.completedFuture(null);
            return;
        }

        CompletableFuture.runAsync(() -> writeSync(input)).exceptionallyAsync(this::onError);
    }

    private void writeSync(Object input) {
        try {
            var gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.DSYNC));
            gzipOutputStream.write(SMILE.writeValueAsBytes(input));
            gzipOutputStream.flush();
            gzipOutputStream.finish();
            gzipOutputStream.close();
        } catch (Throwable exception) {
            throw new RuntimeException("Cannot write to file", exception);
        }
    }

    private Void onError(Throwable exception) {
        exception.printStackTrace();
        return null;
    }
}
