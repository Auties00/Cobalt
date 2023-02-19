package it.auties.whatsapp.serialization;

import it.auties.whatsapp.util.Validate;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class to load all controller providers
 */
@UtilityClass
public class ControllerProviderLoader {
    private final ServiceLoader<ControllerProvider> LOADER = ServiceLoader.load(ControllerProvider.class);
    private final DefaultControllerProvider DEFAULT_SERIALIZER = new DefaultControllerProvider();
    private List<ControllerSerializerProvider> cachedSerializers;
    private ControllerDeserializerProvider cachedDeserializer;

    /**
     * Returns all the IDs
     *
     * @param useDefault whether the default serializer should be included
     * @return a non-null list
     */
    public LinkedList<Integer> findAllIds(boolean useDefault) {
        return stream(useDefault).map(ControllerProvider::findIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Streams all the providers loaded in the class loader
     *
     * @param useDefault whether the default serializer should be included
     * @return a non-null stream
     */
    public Stream<ControllerProvider> stream(boolean useDefault) {
        var providers = LOADER.stream().map(ServiceLoader.Provider::get);
        return useDefault ? Stream.concat(Stream.of(DEFAULT_SERIALIZER), providers) : providers;
    }

    /**
     * Returns all the serializers
     *
     * @param useDefault whether the default serializer should be included
     * @return a non-null list
     */
    public List<ControllerSerializerProvider> findAllSerializers(boolean useDefault) {
        return cachedSerializers != null ? cachedSerializers : (cachedSerializers = findAllSerializersInternal(useDefault));
    }

    private List<ControllerSerializerProvider> findAllSerializersInternal(boolean useDefault) {
        return ControllerProviderLoader.stream(useDefault)
                .filter(entry -> entry instanceof ControllerSerializerProvider)
                .map(entry -> (ControllerSerializerProvider) entry)
                .toList();
    }

    /**
     * Returns the best deserializer available
     *
     * @param useDefault whether the default serializer should be included
     * @return a non-null list
     */
    public ControllerDeserializerProvider findOnlyDeserializer(boolean useDefault) {
        return cachedDeserializer != null ? cachedDeserializer : (cachedDeserializer = findOnlyDeserializerInternal(useDefault));
    }

    private ControllerDeserializerProvider findOnlyDeserializerInternal(boolean useDefault) {
        var providers = ControllerProviderLoader.stream(useDefault)
                .filter(entry -> entry instanceof ControllerDeserializerProvider)
                .map(entry -> (ControllerDeserializerProvider) entry)
                .collect(Collectors.toCollection(LinkedList::new));
        Validate.isTrue(!providers.isEmpty(), "No deserializers were found", NoSuchElementException.class);
        var best = providers.stream()
                .filter(ControllerDeserializerProvider::isBest)
                .collect(Collectors.toCollection(LinkedList::new));
        if (best.isEmpty()) {
            checkProviders(providers, "Cannot resolve conflicts between serializers, more than one serializer with default priority exists: %s");
            return providers.getFirst();
        }
        checkProviders(best, "Cannot resolve conflicts between serializers, more than one serializer with high priority exists: %s");
        return best.getFirst();
    }

    private void checkProviders(Collection<? extends ControllerProvider> providers, String message) {
        Validate.isTrue(providers.size() == 1, message, UnsupportedOperationException.class, providers.stream()
                .map(Object::getClass)
                .map(Class::getName)
                .collect(Collectors.joining(", ")));
    }
}
