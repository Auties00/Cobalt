package it.auties.whatsapp4j.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Validate {
    public void isTrue(boolean value, @NotNull String message, Object... args) {
        if(!value) throw new SecurityException(message.formatted(args));
    }

    public void ifTrue(boolean value, Runnable action) {
        if(value) action.run();
    }
}
