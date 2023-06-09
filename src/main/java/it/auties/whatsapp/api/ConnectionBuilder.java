package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.DefaultControllerSerializer;
import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

/**
 * A builder to specify the type of connection to use
 *
 * @param <T> the type of the result
 */
@SuppressWarnings("unused")
public final class ConnectionBuilder<T extends OptionsBuilder<T>> {
    private final ClientType clientType;
    private ControllerSerializer serializer;
    ConnectionBuilder(@NonNull ClientType clientType){
        this.clientType = clientType;
        this.serializer = DefaultControllerSerializer.instance();
    }

    /**
     * Uses a custom serializer
     *
     * @param serializer the non-null serializer to use
     * @return the same instance for chaining
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
        return newConnection((UUID) null);
    }

    /**
     * Creates a new connection using a unique identifier
     * If a session with the given id already exists, it will be retrieved.
     * Otherwise, a new one will be created.
     *
     * @param uuid the nullable uuid to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(UUID uuid) {
        return createConnection(uuid, ConnectionType.NEW);
    }

    /**
     * Creates a new connection using a phone number
     * If a session with the given phone number already exists, it will be retrieved.
     * Otherwise, a new one will be created.
     *
     * @param phoneNumber the nullable uuid to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(long phoneNumber) {
        return createConnection(phoneNumber);
    }

    /**
     * Creates a new connection using an alias
     * If a session with the given alias already exists, it will be retrieved.
     * Otherwise, a new one will be created.
     *
     * @param alias the nullable alias to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(String alias) {
        return createConnection(alias);
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, an empty optional will be returned
     *
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(UUID uuid) {
        return createOptionalConnection(uuid, ConnectionType.NEW);
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, an empty optional will be returned
     *
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(Long phoneNumber) {
        return createOptionalConnection(phoneNumber);
    }

    /**
     * Creates a new connection using an alias
     * If no connection is available, an empty optional will be returned
     *
     * @param alias the nullable alias to use to create the connection
     * @return a non-null options selector
     */
    public Optional<T>  newOptionalConnection(String alias) {
        return createOptionalConnection(alias);
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
     * Creates a new connection from the first connection that was serialized
     *
     * @return an optional
     */
    public Optional<T> firstOptionalConnection() {
        return createOptionalConnection(null, ConnectionType.FIRST);
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

    /**
     * Creates a new connection from the last connection that was serialized
     *
     * @return an optional
     */
    public Optional<T> lastOptionalConnection() {
        return createOptionalConnection(null, ConnectionType.LAST);
    }

    @SuppressWarnings("unchecked")
    private T createConnection(UUID uuid, ConnectionType connectionType) {
        return (T) switch (clientType) {
            case WEB -> WebOptionsBuilder.of(uuid, serializer, connectionType);
            case MOBILE -> MobileOptionsBuilder.of(uuid, serializer, connectionType);
        };
    }

    @SuppressWarnings("unchecked")
    private T createConnection(long phoneNumber) {
        return (T) switch (clientType) {
            case WEB -> WebOptionsBuilder.of(phoneNumber, serializer);
            case MOBILE -> MobileOptionsBuilder.of(phoneNumber, serializer);
        };
    }

    @SuppressWarnings("unchecked")
    private T createConnection(String alias) {
        return (T) switch (clientType) {
            case WEB -> WebOptionsBuilder.of(alias, serializer);
            case MOBILE -> MobileOptionsBuilder.of(alias, serializer);
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<T> createOptionalConnection(UUID uuid, ConnectionType connectionType) {
        return (Optional<T>) switch (clientType) {
            case WEB -> WebOptionsBuilder.ofNullable(uuid, serializer, connectionType);
            case MOBILE -> MobileOptionsBuilder.ofNullable(uuid, serializer, connectionType);
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<T> createOptionalConnection(long phoneNumber) {
        return (Optional<T>) switch (clientType) {
            case WEB -> WebOptionsBuilder.ofNullable(phoneNumber, serializer);
            case MOBILE -> MobileOptionsBuilder.ofNullable(phoneNumber, serializer);
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<T> createOptionalConnection(String alias) {
        return (Optional<T>) switch (clientType) {
            case WEB -> WebOptionsBuilder.ofNullable(alias, serializer);
            case MOBILE -> MobileOptionsBuilder.ofNullable(alias, serializer);
        };
    }
}
