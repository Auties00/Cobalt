package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
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
    @Override
    @SneakyThrows
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        var methods = findCheckerMethods();
        var propertyChecker = methods.stream().filter(this::invokeCheckerMethod).findAny();
        if(propertyChecker.isEmpty()){
            return Optional.empty();
        }

        var propertyGetter = findGetterMethod(propertyChecker.get());
        var property = propertyGetter.invoke(info.getMessage());

        return findContextInfoMethod(property).map(method -> invokeContextInfoGetter(property, method));
    }

    private @NotNull Method findGetterMethod(@NotNull Method propertyChecker) throws NoSuchMethodException {
        return info.getMessage().getClass().getMethod(propertyChecker.getName().replaceFirst("has", "get"));
    }

    private @NotNull List<Method> findCheckerMethods() {
        return Arrays.stream(info.getMessage().getClass().getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("has"))
                .toList();
    }

    @SneakyThrows
    private boolean invokeCheckerMethod(@NotNull Method method) {
        return (boolean) method.invoke(info.getMessage());
    }

    private @NotNull Optional<Method> findContextInfoMethod(@NotNull Object property){
        try {
            return Optional.of(property.getClass().getMethod("hasContextInfo"));
        }catch (NoSuchMethodException ex){
            return Optional.empty();
        }
    }

    @SneakyThrows
    private @NotNull WhatsappProtobuf.ContextInfo invokeContextInfoGetter(@NotNull Object property, @NotNull Method method) {
        return (WhatsappProtobuf.ContextInfo) method.invoke(property);
    }
}
