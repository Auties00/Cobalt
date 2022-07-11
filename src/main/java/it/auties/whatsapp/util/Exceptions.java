package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@UtilityClass
public class Exceptions {
    public Throwable current(){
        var trace = Thread.currentThread().getStackTrace();
        var result = new RuntimeException();
        result.setStackTrace(trace);
        return result;
    }

    public Path save(Throwable throwable) {
        var actual = Objects.requireNonNullElseGet(throwable, RuntimeException::new);
        try {
            var path = Preferences.home()
                    .resolve("exceptions");
            Files.createDirectories(path);
            var file = path.resolve("%s.txt".formatted(System.currentTimeMillis()));
            var stackTraceWriter = new StringWriter();
            var stackTracePrinter = new PrintWriter(stackTraceWriter);
            actual.printStackTrace(stackTracePrinter);
            Files.writeString(file, stackTraceWriter.toString());
            return file;
        } catch (Throwable ignored) {
            throw new RuntimeException("Cannot serialize exception. Here is the non-serialized stack trace", actual);
        }
    }
}
