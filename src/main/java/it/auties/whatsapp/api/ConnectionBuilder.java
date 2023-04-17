package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.DefaultControllerSerializer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

/**
 * A builder to specify the type of connection to use
 *
 * @param <T> the type of the result
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ConnectionBuilder<T extends OptionsBuilder<T>> {
    private static final ControllerSerializer DEFAULT_SERIALIZER = new DefaultControllerSerializer();

    private final ClientType clientType;
    private ControllerSerializer serializer;

    /**
     * Uses a custom serializer
     *
     * @param serializer the non-null serializer to use
     * @return an options builder
     */
    public ConnectionBuilder<T> serializer(@NonNull ControllerSerializer serializer) {
       this.serializer = serializer;
       return this;
    }

    /**
     * Creates a new connection using a random uuid
     *
     * @return a non-null options selector
     */
    public T newConnection() {
        return knownConnection(UUID.randomUUID());
    }

    /**
     * Creates a new connection using a supplied uuid
     * If a connection was serialized in the past with that uuid, it will be retrieved, otherwise a new one will be started
     *
     * @param uuid the non-null uuid to use
     * @return a non-null options selector
     */
    public T knownConnection(@NonNull UUID uuid) {
        return createConnection(uuid, ConnectionType.NEW);
    }

    /**
     * Creates a new connection from the first connection that was serialized
     * If no connection is available, a new one will be created
     *
     * @return a non-null options selector
     */
    public T firstConnection() {
        return createConnection(null, ConnectionType.FIRST);
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, a new one will be created
     *
     * @return a non-null options selector
     */
    public T lastConnection() {
        return createConnection(null, ConnectionType.LAST);
    }

    @SuppressWarnings("unchecked")
    private T createConnection(UUID uuid, ConnectionType connectionType) {
        return (T) switch (clientType) {
            case WEB_CLIENT ->
                    new WebOptionsBuilder(uuid, Objects.requireNonNullElse(serializer, DEFAULT_SERIALIZER), connectionType);
            case APP_CLIENT ->
                    new MobileOptionsBuilder(uuid, Objects.requireNonNullElse(serializer, DEFAULT_SERIALIZER), connectionType);
        };
    }
}
