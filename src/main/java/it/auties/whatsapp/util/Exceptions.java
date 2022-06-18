package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class Exceptions {
    public Path save(Throwable throwable){
        var actual = Objects.requireNonNullElseGet(throwable, RuntimeException::new);
        try {
            var path = Preferences.home().resolve("exceptions");
            Files.createDirectories(path);
            var file = path.resolve("%s.txt".formatted(System.currentTimeMillis()));
            var stackTraceWriter = new StringWriter();
            var stackTracePrinter = new PrintWriter(stackTraceWriter);
            actual.printStackTrace(stackTracePrinter);
            Files.writeString(file, stackTraceWriter.toString());
            return file;
        }catch (Throwable ignored){
            throw new RuntimeException("Cannot serialize exception. Here is the non-serialized stack trace", actual);
        }
    }

    public <T extends Throwable> T make(T baseException, List<? extends Throwable> exceptions) {
        var all = new ArrayList<Throwable>(exceptions);
        all.add(0, baseException);
        var iterator = all.iterator();
        var registeredMessages = new HashSet<String>();
        while (iterator.hasNext()) {
            var current = findInnerException(iterator.next());
            if (!iterator.hasNext()) {
                break;
            }

            var cause = iterator.next();
            if (current == cause || registeredMessages.contains(cause.getMessage())) {
                continue;
            }

            registeredMessages.add(cause.getMessage());
            current.initCause(cause);
        }

        return baseException;
    }

    private Throwable findInnerException(Throwable throwable) {
        return throwable.getCause() == null ?
                throwable :
                findInnerException(throwable.getCause());
    }
}
