package it.auties.whatsapp4j.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Validate {
    public void isTrue(boolean value, @NotNull String message, @NotNull Object... args) {
        if(!value) throw new IllegalArgumentException(message.formatted(args));
    }
}
