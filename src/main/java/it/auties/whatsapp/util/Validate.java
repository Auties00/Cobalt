package it.auties.whatsapp.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Constructor;
import java.util.Arrays;

@UtilityClass
public class Validate {
    public void isTrue(boolean value, @NonNull String message, Object... args) {
        isTrue(value, message, IllegalArgumentException.class, args);
    }

    @SuppressWarnings("unchecked")
    public <T extends Throwable> void isTrue(boolean value, @NonNull String message, @NonNull Class<? extends Throwable> throwable, Object... args) throws T {
        if (value) {
            return;
        }
        throw (T) createThrowable(throwable, message.formatted(args));
    }

    private Throwable createThrowable(Class<? extends Throwable> throwable, String formattedMessage) {
        try {
            var result = throwable.getConstructor(String.class).newInstance(formattedMessage);
            var stackTrace = Arrays.stream(result.getStackTrace())
                    .filter(entry -> !entry.getClassName().equals(Validate.class.getName()) && !entry.getClassName().equals(Constructor.class.getName()))
                    .toArray(StackTraceElement[]::new);
            result.setStackTrace(stackTrace);
            return result;
        } catch (Throwable ignored) {
            return new RuntimeException(formattedMessage);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Throwable> void isTrue(boolean value, @NonNull Throwable throwable) throws T {
        if (value) {
            return;
        }
        throw (T) throwable;
    }
}
