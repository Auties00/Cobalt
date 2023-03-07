package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

@UtilityClass
public class Exceptions {
    public Throwable current(String message) {
        var result = new RuntimeException(message);
        result.setStackTrace(currentStackTrace());
        return result;
    }

    private StackTraceElement[] currentStackTrace() {
        var stackTrace = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stackTrace, 3, stackTrace.length);
    }

    public Path save(Throwable throwable) {
        try {
            var actual = Objects.requireNonNullElseGet(throwable, RuntimeException::new);
            var path = Files.createTempFile("whatsapp4j", ".txt");
            var stackTraceWriter = new StringWriter();
            var stackTracePrinter = new PrintWriter(stackTraceWriter);
            actual.printStackTrace(stackTracePrinter);
            Files.writeString(path, stackTraceWriter.toString());
            return path;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot serialize exception. Here is the non-serialized stack trace", exception);
        }
    }
}
