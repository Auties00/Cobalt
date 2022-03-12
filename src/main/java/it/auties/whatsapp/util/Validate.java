package it.auties.whatsapp.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import static lombok.Lombok.sneakyThrow;

@UtilityClass
public class Validate {
    public void isTrue(boolean value, @NonNull String message, Object... args) {
        isTrue(value, message, IllegalArgumentException.class, args);
    }

    public void isTrue(boolean value, @NonNull String message, @NonNull Class<? extends Throwable> throwable, Object... args) {
        isTrue(value, createThrowable(throwable, message.formatted(args)));
    }

    public void isTrue(boolean value, @NonNull Throwable throwable) {
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
