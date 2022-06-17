package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Exceptions {
    public <T extends Throwable> T make(T baseException, List<? extends Throwable> exceptions) {
        var all = new ArrayList<Throwable>(exceptions);
        all.add(0, baseException);
        var iterator = all.iterator();
        while (iterator.hasNext()) {
            var current = findInnerException(iterator.next());
            if (!iterator.hasNext()) {
                break;
            }

            var cause = iterator.next();
            if (current == cause) {
                continue;
            }

            current.initCause(cause);
        }

        return baseException;
    }

    private Throwable findInnerException(Throwable throwable) {
        return throwable.getCause() == null ?
                throwable :
                findInnerException(throwable.getCause());
    }
}
