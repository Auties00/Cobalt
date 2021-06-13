package it.auties.whatsapp4j.utils.internal;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * This utility class provides an easy way to check if a condition is satisfied
 * If the condition isn't satisfied, an exception is thrown
 * Apache Commons or any other similar library to reduce the size of this library
 */
@UtilityClass
public class Validate {
    /**
     * Throws an exception of type IllegalArgumentException with message {@code message} formatted using {@code args} if {@code value} is not true.
     * Otherwise returns {@code object}
     *
     * @param object    the object to return if the check is successful
     * @param condition the value to check
     * @param message   the message of the exception to throw if {@code value} isn't true
     * @param args      the arguments used to format the exception thrown if {@code value} isn't true
     */
    public <T> T isValid(T object, boolean condition, @NonNull String message, @NonNull Object... args) {
        isTrue(condition, message, IllegalArgumentException.class, args);
        return object;
    }

    /**
     * Throws an exception of type IllegalArgumentException with message {@code message} formatted using {@code args} if {@code value} is not true
     *
     * @param value   the value to check
     * @param message the message of the exception to throw if {@code value} isn't true
     * @param args    the arguments used to format the exception thrown if {@code value} isn't true
     */
    public void isTrue(boolean value, @NonNull String message, @NonNull Object... args) {
        isTrue(value, message, IllegalArgumentException.class, args);
    }

    /**
     * Throws an exception of type {@code exception} with message {@code message} formatted using {@code args} if {@code value} is not true
     *
     * @param value     the value to check
     * @param message   the message of the exception to throw if {@code value} isn't true
     * @param exception the type of exception to throw if {@code value} isn't true
     * @param args      the arguments used to format the exception thrown if {@code value} isn't true
     */
    @SneakyThrows
    public void isTrue(boolean value, @NonNull String message, @NonNull Class<? extends Exception> exception, @NonNull Object... args) {
        if (!value) throw exception.getConstructor(String.class).newInstance(message.formatted(args));
    }
}
