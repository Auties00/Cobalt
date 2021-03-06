package it.auties.whatsapp4j.annotation;

import org.jetbrains.annotations.NotNull;

public class MissingConstructorException extends RuntimeException{
    public MissingConstructorException(@NotNull String message, Object... args) {
        super(message.formatted(args));
    }
}
