package it.auties.whatsapp4j.listener;

import lombok.experimental.UtilityClass;
import jakarta.validation.constraints.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A utility class to find all classes annotated with {@link RegisterListener}.
 * This class uses Google's reflections library to find said classes anywhere in the project.
 */
@UtilityClass
public class RegisterListenerProcessor {
    /**
     * An instance of Reflections, used to visit all classes loaded by the class loader
     */
    private final Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setScanners(new SubTypesScanner(false), new ResourcesScanner(), new TypeAnnotationsScanner())
            .setUrls(ClasspathHelper.forJavaClassPath()));

    /**
     * Queries all classes annotated with {@link RegisterListener} and initializes them using a no args constructor
     *
     * @return a list of {@link WhatsappListener}
     */
    public @NotNull List<WhatsappListener> queryAllListeners() {
        return reflections.getTypesAnnotatedWith(RegisterListener.class)
                .stream()
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
