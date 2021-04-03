package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a miscellaneous message inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class WhatsappGenericMessage extends WhatsappUserMessage {
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
        var methods = ReflectionUtils.getMethods(info.getMessage().getClass(), ReflectionUtils.withModifier(Modifier.PUBLIC), ReflectionUtils.withPrefix("has"));
        var propertyChecker = methods.stream().filter(this::invoke).findAny();
        if(propertyChecker.isEmpty()){
            return Optional.empty();
        }

        var propertyGetter = info.getMessage().getClass().getMethod(propertyChecker.get().getName().replaceFirst("has", "get"));
        var property = propertyGetter.invoke(info.getMessage());
        var contextChecker = ReflectionUtils.getMethods(property.getClass(), ReflectionUtils.withModifier(Modifier.PUBLIC), ReflectionUtils.withName("hasContextInfo"));
        return contextChecker.isEmpty() ? Optional.empty() : Optional.of((WhatsappProtobuf.ContextInfo) property.getClass().getMethod("getContextInfo").invoke(property));
    }

    @SneakyThrows
    private boolean invoke(Method method) {
        return (boolean) method.invoke(info.getMessage());
    }
}
