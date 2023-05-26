package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.DefaultControllerSerializer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A builder to specify the type of connection to use
 *
 * @param <T> the type of the result
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ConnectionBuilder<T extends OptionsBuilder<T>> {
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
        return newConnection(null);
    }

    /**
     * Creates a new connection using a unique identifier
     *
     * @param uuid the nullable uuid to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(UUID uuid) {
        return createConnection(uuid, ConnectionType.NEW).get();
    }

    /**
     * Creates a new connection using a random uuid
     *
     * @param phoneNumber the nullable uuid to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(long phoneNumber) {
        return createConnection(phoneNumber, ConnectionType.NEW).get();
    }

    /**
     * Creates a new connection from the first connection that was serialized
     * If no connection is available, a new one will be created
     *
     * @return a non-null options selector
     */
    public T firstConnection() {
        return createConnection(null, ConnectionType.FIRST).get();
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, a new one will be created
     *
     * @return a non-null options selector
     */
    public T lastConnection() {
        return createConnection(null, ConnectionType.LAST).get();
    }

    /**
     * Creates a new connection using a supplied uuid
     *
     * @param uuid the non-null uuid to use
     * @return a non-null options selector
     */
    public Optional<T> knownConnection(@NonNull UUID uuid) {
        return createConnection(uuid, ConnectionType.KNOWN);
    }

    /**
     * Creates a new connection using a supplied phone number
     *
     * @param phoneNumber the non-null phone number
     * @return a non-null options selector
     */
    public Optional<T> knownConnection(long phoneNumber) {
        return createConnection(phoneNumber, ConnectionType.KNOWN);
    }

    @SuppressWarnings("unchecked")
    private Optional<T> createConnection(UUID uuid, ConnectionType connectionType) {
        var serializer = Objects.requireNonNullElse(this.serializer, DefaultControllerSerializer.instance());
        return (Optional<T>) switch (clientType) {
            case WEB -> WebOptionsBuilder.of(uuid, serializer, connectionType);
            case MOBILE -> MobileOptionsBuilder.of(uuid, serializer, connectionType);
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<T> createConnection(long phoneNumber, ConnectionType connectionType) {
        var serializer = Objects.requireNonNullElse(this.serializer, DefaultControllerSerializer.instance());
        return (Optional<T>) switch (clientType) {
            case WEB -> WebOptionsBuilder.of(phoneNumber, serializer, connectionType);
            case MOBILE -> MobileOptionsBuilder.of(phoneNumber, serializer, connectionType);
        };
    }
}
