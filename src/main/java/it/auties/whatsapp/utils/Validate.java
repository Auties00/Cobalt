package it.auties.whatsapp.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import static lombok.Lombok.sneakyThrow;

/**
 * This utility class provides an easy way to check if a condition is satisfied
 * If the condition isn't satisfied, an exception is thrown
 */
@UtilityClass
public class Validate {
    /**
     * Throws an exception with type IllegalArgumentException with message {@code message} formatted using {@code args} if {@code value} is not true.
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
     * Throws an exception with type IllegalArgumentException with message {@code message} formatted using {@code args} if {@code value} is not true
     *
     * @param value   the value to check
     * @param message the message of the exception to throw if {@code value} isn't true
     * @param args    the arguments used to format the exception thrown if {@code value} isn't true
     */
    public void isTrue(boolean value, @NonNull String message, @NonNull Object... args) {
        isTrue(value, message, IllegalArgumentException.class, args);
    }

    /**
     * Throws an exception with type {@code exception} with message {@code message} formatted using {@code args} if {@code value} is not true.
     * If the provided exception doesn't provide a message constructor, a {@link RuntimeException} will be thrown instead.
     *
     * @param value     the value to check
     * @param message   the message of the exception to throw if {@code value} isn't true
     * @param throwable the type of exception to throw if {@code value} isn't true
     * @param args      the arguments used to format the exception thrown if {@code value} isn't true
     */
    public void isTrue(boolean value, @NonNull String message, @NonNull Class<? extends Throwable> throwable, @NonNull Object... args) {
        isTrue(value, createThrowable(throwable, message.formatted(args)));
    }

    /**
     * Throws {@code exception} if {@code value} is not true.
     *
     * @param value     the value to check
     * @param throwable the exception to throw
     */
    public void isTrue(boolean value, Throwable throwable) {
        if (value) {
            return;
        }

        throw sneakyThrow(throwable);
    }

    private Throwable createThrowable(Class<? extends Throwable> throwable, String formattedMessage) {
        try {
            return throwable.getConstructor(String.class)
                    .newInstance(formattedMessage);
        }catch (Throwable ignored){
            return new RuntimeException(formattedMessage);
        }
    }
}
