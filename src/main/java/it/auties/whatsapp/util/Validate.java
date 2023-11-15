package it.auties.whatsapp.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Supplier;

public final class Validate {
    public static void isTrue(boolean value, String message, Object... args) {
        isTrue(value, message, IllegalArgumentException.class, args);
    }

    public static <T extends Throwable> void isTrue(boolean value, Class<? extends Throwable> throwable) throws T {
        isTrue(value, null, throwable);
    }

    public static <T extends Throwable> void isTrue(boolean value, Supplier<T> throwableSupplier) throws T {
        if (value) {
            return;
        }

        throw throwableSupplier.get();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void isTrue(boolean value, String message, Class<? extends Throwable> throwable, Object... args) throws T {
        if (value) {
            return;
        }

        throw (T) createThrowable(throwable, message == null ? null : message.formatted(args));
    }

    private static Throwable createThrowable(Class<? extends Throwable> throwable, String formattedMessage) {
        try {
            var result = formattedMessage == null ? throwable.getConstructor().newInstance() : throwable.getConstructor(String.class).newInstance(formattedMessage);
            var stackTrace = Arrays.stream(result.getStackTrace())
                    .filter(entry -> !entry.getClassName().equals(Validate.class.getName()) && !entry.getClassName().equals(Constructor.class.getName()))
                    .toArray(StackTraceElement[]::new);
            result.setStackTrace(stackTrace);
            return result;
        } catch (Throwable ignored) {
            return new RuntimeException(formattedMessage);
        }
    }
}
