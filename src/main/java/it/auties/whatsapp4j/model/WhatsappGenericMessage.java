package it.auties.whatsapp4j.model;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a miscellaneous message inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class WhatsappGenericMessage extends WhatsappUserMessage<WhatsappGenericMessage> {
    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappGenericMessage(WhatsappProtobuf.@NotNull WebMessageInfo info) {
        super(info, info.hasMessage());
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @SuppressWarnings("unchecked")
    @Override
    @SneakyThrows
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        var scanResult = new ClassGraph().acceptClasses(info.getMessage().getClass().getCanonicalName())
                .enableAllInfo()
                .scan();
        ClassInfo classInfo = scanResult.getClassInfo(info.getMessage().getClass().getCanonicalName());
        Set<Method> methods = classInfo.getMethodInfo().stream().map(a -> a.loadClassAndGetMethod()).collect(Collectors.toSet());

        //var methods = ReflectionUtils.getMethods(info.getMessage().getClass(), ReflectionUtils.withModifier(Modifier.PUBLIC), ReflectionUtils.withPrefix("has"));
        var propertyChecker = methods.stream().filter(this::invoke).findAny();
        if(propertyChecker.isEmpty()){
            return Optional.empty();
        }
        var propertyGetter = info.getMessage().getClass().getMethod(propertyChecker.get().getName().replaceFirst("has", "get"));
        var property = propertyGetter.invoke(info.getMessage());
        var propertyResult = new ClassGraph().acceptClasses(property.getClass().getCanonicalName())
                .enableMethodInfo()
                .scan();
        ClassInfo propertyClassInfo = propertyResult.getClassInfo(property.getClass().getCanonicalName());
        Optional<MethodInfo> hasContextInfo = propertyClassInfo.getMethodInfo().stream().filter(a -> a.isPublic() && a.getName().equals("hasContextInfo")).findAny();
        return hasContextInfo.isEmpty() ? Optional.empty() : Optional.of((WhatsappProtobuf.ContextInfo) property.getClass().getMethod("getContextInfo").invoke(property));
    }

    @SneakyThrows
    private boolean invoke(Method method) {
        return (boolean) method.invoke(info.getMessage());
    }
}
