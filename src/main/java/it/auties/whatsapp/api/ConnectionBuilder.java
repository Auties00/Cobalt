package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.util.ControllerHelper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A builder to specify the type of connection to use
 *
 * @param <T> the type of the newsletters
 */
@SuppressWarnings("unused")
public final class ConnectionBuilder<T extends OptionsBuilder<T>> {
    private final ClientType clientType;
    private ControllerSerializer serializer;

    ConnectionBuilder(ClientType clientType) {
        this.clientType = clientType;
        this.serializer = ControllerSerializer.toSmile();
    }

    /**
     * Uses a custom serializer
     *
     * @param serializer the non-null serializer to use
     * @return the same instance for chaining
     */
    public ConnectionBuilder<T> serializer(ControllerSerializer serializer) {
        this.serializer = serializer;
        return this;
    }

    /**
     * Creates a new connection using a random uuid
     *
     * @return a non-null options selector
     */
    public T newConnection() {
        return newConnection(UUID.randomUUID());
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
        var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        var sessionStoreAndKeys = ControllerHelper.deserialize(sessionUuid, null, null, clientType, serializer)
                .orElseGet(() -> ControllerHelper.create(sessionUuid, null, null, clientType, serializer));
        return createConnection(sessionStoreAndKeys);
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
        var sessionStoreAndKeys = ControllerHelper.deserialize(null, phoneNumber, null, clientType, serializer)
                .orElseGet(() -> ControllerHelper.create(UUID.randomUUID(), phoneNumber, null, clientType, serializer));
        return createConnection(sessionStoreAndKeys);
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
        var sessionStoreAndKeys = ControllerHelper.deserialize(null, null, alias, clientType, serializer)
                .orElseGet(() -> ControllerHelper.create(UUID.randomUUID(), null, alias != null ? List.of(alias) : null, clientType, serializer));
        return createConnection(sessionStoreAndKeys);
    }

    /**
     * Creates a new connection from the first connection that was serialized
     * If no connection is available, a new one will be created
     *
     * @return a non-null options selector
     */
    public T firstConnection() {
        return newConnection(serializer.listIds(clientType).peekFirst());
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, a new one will be created
     *
     * @return a non-null options selector
     */
    public T lastConnection() {
        return newConnection(serializer.listIds(clientType).peekLast());
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, an empty optional will be returned
     *
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(UUID uuid) {
        var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        var sessionStoreAndKeys = ControllerHelper.deserialize(sessionUuid, null, null, clientType, serializer);
        return sessionStoreAndKeys.map(this::createConnection);
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, an empty optional will be returned
     *
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(Long phoneNumber) {
        var sessionStoreAndKeys = ControllerHelper.deserialize(null, phoneNumber, null, clientType, serializer);
        return sessionStoreAndKeys.map(this::createConnection);
    }

    /**
     * Creates a new connection using an alias
     * If no connection is available, an empty optional will be returned
     *
     * @param alias the nullable alias to use to create the connection
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(String alias) {
        var sessionStoreAndKeys = ControllerHelper.deserialize(null, null, alias, clientType, serializer);
        return sessionStoreAndKeys.map(this::createConnection);
    }

    /**
     * Creates a new connection from the first connection that was serialized
     *
     * @return an optional
     */
    public Optional<T> firstOptionalConnection() {
        return newOptionalConnection(serializer.listIds(clientType).peekFirst());
    }

    /**
     * Creates a new connection from the last connection that was serialized
     *
     * @return an optional
     */
    public Optional<T> lastOptionalConnection() {
        return newOptionalConnection(serializer.listIds(clientType).peekLast());
    }

    @SuppressWarnings("unchecked")
    private T createConnection(ControllerHelper.StoreAndKeysPair sessionStoreAndKeys) {
        return (T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
            case MOBILE -> new MobileOptionsBuilder(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
        };
    }
}
