package it.auties.whatsapp4j.listener;

import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A utility class to find all classes annotated with {@link RegisterListener}.
 */
@UtilityClass
public class RegisterListenerProcessor {
    private final ClassLoader CLASS_LOADER = ClassLoader.getSystemClassLoader();
    private final StandardJavaFileManager FILE_MANAGER = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, Locale.getDefault(), StandardCharsets.UTF_8);
    private final StandardLocation CLASS_LOCATION = StandardLocation.CLASS_PATH;

    /**
     * Queries all classes annotated with {@link RegisterListener} and initializes them using a no args constructor
     *
     * @return a list of {@link WhatsappListener}
     */
    public @NotNull List<WhatsappListener> queryAllListeners() {
        return Arrays.stream(CLASS_LOADER.getDefinedPackages())
                .flatMap(RegisterListenerProcessor::findClassesInPackage)
                .filter(RegisterListenerProcessor::isListener)
                .map(RegisterListenerProcessor::cast)
                .map(RegisterListenerProcessor::newInstance)
                .toList();
    }

    @SneakyThrows
    private @NotNull Stream<Class<?>> findClassesInPackage(@NotNull Package pack){
        return StreamSupport
                .stream(FILE_MANAGER.list(CLASS_LOCATION, pack.getName(), Set.of(JavaFileObject.Kind.CLASS), true).spliterator(), true)
                .map(RegisterListenerProcessor::loadClassFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private @NotNull Optional<Class<?>> loadClassFromFile(@NotNull JavaFileObject file) {
        try {
            return Optional.of(Class.forName(FILE_MANAGER.inferBinaryName(CLASS_LOCATION, file)));
        }catch (ClassNotFoundException | NoClassDefFoundError error) {
            return Optional.empty();
        }
    }

    private boolean isListener(@NotNull Class<?> clazz) {
        return clazz.isAnnotationPresent(RegisterListener.class) && WhatsappListener.class.isAssignableFrom(clazz);
    }

    private @NotNull Class<? extends WhatsappListener> cast(@NotNull Class<?> clazz){
        try{
            return clazz.asSubclass(WhatsappListener.class);
        }catch (ClassCastException ex){
            throw new RuntimeException("WhatsappAPI: Cannot initialize class %s, classes annotated with @RegisterListener should implement WhatsappListener(how did this even get through?)".formatted(clazz.getName()));
        }
    }

    private @NotNull WhatsappListener newInstance(@NotNull Class<? extends WhatsappListener> clazz){
        try {
            return clazz.getDeclaredConstructor().newInstance();
        }catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("WhatsappAPI: Cannot initialize class %s%s".formatted(clazz.getName(), parseError(e)));
        }
    }

    private @NotNull String parseError(@NotNull ReflectiveOperationException e) {
        return Optional.ofNullable(e.getMessage()).map(" with error %s"::formatted).orElse("");
    }
}
