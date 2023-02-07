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

final class SmileFile
    implements JacksonProvider {
  private static final Map<Path, CompletableFuture<?>> futures = new ConcurrentHashMap<>();

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
    return of(LocalFileSystem.of(path.formatted(args))
        .toAbsolutePath());
  }

  public static SmileFile of(Path path) {
    return new SmileFile(path);
  }

  public <T> Optional<T> read(Class<T> clazz)
      throws IOException {
    if (Files.notExists(file)) {
      return Optional.empty();
    }
    var stream = Files.newInputStream(file);
    return Optional.of(SMILE.readValue(new GZIPInputStream(stream, 65536), clazz));
  }

  public void write(Object input, boolean async) {
    var oldFuture = futures.get(file);
    if (!async) {
      if(oldFuture != null){
        oldFuture.cancel(true);
      }

      writeSync(input);
      return;
    }

    Runnable worker = () -> writeSync(input);
    var future = oldFuture != null ? oldFuture.thenRunAsync(worker) : CompletableFuture.runAsync(worker);
    futures.put(file, future.exceptionallyAsync(this::onError));
  }

  private Void onError(Throwable exception) {
    exception.printStackTrace();
    return null;
  }

  private void writeSync(Object input) {
    try {
      var gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.DSYNC));
      gzipOutputStream.write(SMILE.writeValueAsBytes(input));
      gzipOutputStream.finish();
    } catch (Throwable exception) {
      throw new RuntimeException("Cannot write to file", exception);
    }
  }
}
