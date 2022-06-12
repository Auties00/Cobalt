package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Exceptions {
    public <T extends Throwable> T make(T baseException, List<? extends Throwable> exceptions) {
        Throwable innerException = baseException;
        while (innerException.getCause() != null) {
            innerException = innerException.getCause();
        }

        var all = new ArrayList<Throwable>(exceptions);
        all.add(0, innerException);
        for (var x = 0; x < all.size() - 1; x++) {
            var current = all.get(x);
            var cause = all.get(x + 1);
            current.initCause(cause);
        }

        return baseException;
    }
}
