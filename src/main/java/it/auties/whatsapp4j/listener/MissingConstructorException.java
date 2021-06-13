package it.auties.whatsapp4j.listener;

import lombok.NonNull;

/**
 * An exception used to signal that a constructor for a specific class with specific characteristics cannot be found.
 * This is an unchecked exception as it extends {@link RuntimeException}.
 * Unchecked exceptions do not need to be declared in a throws clause.
 */
public class MissingConstructorException extends RuntimeException {
    /**
     * Constructs a new missing constructor exception with a not null message formatted using the args parameter
     *
     * @param message the exception's message
     * @param args    the arguments used to format the message
     */
    public MissingConstructorException(@NonNull String message, Object... args) {
        super(message.formatted(args));
    }
}
