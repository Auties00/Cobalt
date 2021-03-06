package it.auties.whatsapp4j.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Validate {
    public void isTrue(boolean value, @NotNull String message, @NotNull Object... args) {
        isTrue(value, message, IllegalArgumentException.class, args);
    }

    @SneakyThrows
    public void isTrue(boolean value, @NotNull String message, Class<? extends Exception> exception, @NotNull Object... args) {
        if(!value) exception.getConstructor(String.class).newInstance(message.formatted(args));
    }
}
