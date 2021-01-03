package it.auties.whatsapp4j.utils;

import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class Validate {
    public void isTrue(boolean value, String message, Object... args) {
        if(!value) throw new SecurityException(message.formatted(args));
    }

    public void notNull(Object object, String message) {
        if(Objects.isNull(object)) throw new SecurityException(message);
    }
}
