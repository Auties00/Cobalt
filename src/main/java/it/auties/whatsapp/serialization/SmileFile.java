package it.auties.whatsapp.serialization;

import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.LocalFileSystem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.NonNull;

final class SmileFile
    implements JacksonProvider {

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
    if (!async) {
      writeSync(input);
      return;
    }
    CompletableFuture.runAsync(() -> writeSync(input));
  }

  private void writeSync(Object input) {
    try {
      if (Files.notExists(file)) {
        Files.createFile(file);
      }
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot prepare file for write", exception);
    }
    try (var channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
      var lock = channel.lock();
      var result = new ByteArrayOutputStream();
      var gzipOutputStream = new GZIPOutputStream(result);
      gzipOutputStream.write(SMILE.writeValueAsBytes(input));
      gzipOutputStream.finish();
      channel.write(ByteBuffer.wrap(result.toByteArray()));
      lock.release();
    } catch (IOException | NonWritableChannelException exception) {
      throw new RuntimeException("Cannot write to file", exception);
    } catch (OverlappingFileLockException ignored) {
    }
  }
}
