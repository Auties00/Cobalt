package it.auties.whatsapp.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public final class Exceptions {
    private static final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.cobalt/errors");

    public static RuntimeException current() {
        var result = new RuntimeException();
        result.setStackTrace(currentStackTrace());
        return result;
    }

    private static StackTraceElement[] currentStackTrace() {
        var stackTrace = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stackTrace, 3, stackTrace.length);
    }

    public static void save(Throwable throwable) {
        save(DEFAULT_DIRECTORY, throwable);
    }

    public static void save(Path directory, Throwable throwable) {
        try {
            var actual = Objects.requireNonNullElseGet(throwable, RuntimeException::new);
            var path = directory.resolve("%s-%s.txt".formatted(actual.getMessage(), UUID.randomUUID()));
            var stackTraceWriter = new StringWriter();
            var stackTracePrinter = new PrintWriter(stackTraceWriter);
            actual.printStackTrace(stackTracePrinter);
            Files.writeString(path, stackTraceWriter.toString(), StandardOpenOption.CREATE);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot serialize exception", exception);
        }
    }
}
