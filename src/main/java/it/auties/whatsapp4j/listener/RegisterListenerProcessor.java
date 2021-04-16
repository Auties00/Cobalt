package it.auties.whatsapp4j.listener;

import io.github.classgraph.ClassGraph;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A utility class to find all classes annotated with {@link RegisterListener}.
 * This class uses Google's reflections library to find said classes anywhere in the project.
 */
@UtilityClass
public class RegisterListenerProcessor {
    /**
     * Queries all classes annotated with {@link RegisterListener} and initializes them using a no args constructor
     *
     * @return a list of {@link WhatsappListener}
     */
    public @NotNull List<WhatsappListener> queryAllListeners() {
        return new ClassGraph().enableClassInfo().enableClassInfo().enableAnnotationInfo().scan()
                .getClassesWithAnnotation(RegisterListener.class.getCanonicalName())
                .stream()
                .map(a-> a.loadClass())
                .map(RegisterListenerProcessor::cast)
                .map(RegisterListenerProcessor::newInstance)
                .toList();
    }

    private @NotNull Class<? extends WhatsappListener> cast(@NotNull Class<?> clazz){
        try{
            return clazz.asSubclass(WhatsappListener.class);
        }catch (ClassCastException ex){
            throw new RuntimeException("WhatsappAPI: Cannot initialize class %s, classes annotated with @RegisterListener should implement WhatsappListener".formatted(clazz.getName()));
        }
    }

    private @NotNull WhatsappListener newInstance(@NotNull Class<? extends WhatsappListener> clazz){
        try {
            return clazz.getDeclaredConstructor().newInstance();
        }catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("WhatsappAPI: Cannot initialize class %s%s".formatted(clazz.getName(), e.getMessage() == null ? "" : " with error %s".formatted(e.getMessage())));
        }
    }
}
