package it.auties.whatsapp4j.annotation;

import it.auties.whatsapp4j.model.WhatsappListener;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

@UtilityClass
public class RegisterListenerProcessor {
    private final Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setScanners(new SubTypesScanner(false), new ResourcesScanner(), new TypeAnnotationsScanner())
            .setUrls(ClasspathHelper.forJavaClassPath()));

    public List<WhatsappListener> queryAllListeners(){
        return reflections.getTypesAnnotatedWith(RegisterListener.class)
                .stream()
                .map(RegisterListenerProcessor::newInstance)
                .collect(Collectors.toUnmodifiableList());
    }

    private WhatsappListener newInstance(@NotNull Class<?> clazz){
        try {
            return (WhatsappListener) Arrays.stream(clazz.getConstructors())
                    .filter(constructor -> constructor.getParameterCount() == 0)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("WhatsappAPI: Cannot initialize listener %s, missing no args constructor".formatted(clazz.getName())))
                    .newInstance();
        }catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("WhatsappAPI: Cannot initialize class %s%s".formatted(clazz.getName(), e.getMessage() == null ? "" : " with error %s".formatted(e.getMessage())));
        }
    }
}
