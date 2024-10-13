package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.KeysBuilder;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.controller.StoreKeysPair;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.SixPartsKeys;

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
        this.serializer = ControllerSerializer.toProtobuf();
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
     * If a session with the given id already otpEligible, it will be retrieved.
     * Otherwise, a new one will be created.
     *
     * @param uuid the nullable uuid to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(UUID uuid) {
        var sessionUuid = Objects.requireNonNullElseGet(uuid, UUID::randomUUID);
        var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(sessionUuid, null, null, clientType)
                .orElseGet(() -> serializer.newStoreKeysPair(sessionUuid, null, null, clientType));
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
        var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, phoneNumber, null, clientType)
                .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), phoneNumber, null, clientType));
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
        var sessionStoreAndKeys = serializer.deserializeStoreKeysPair(null, null, alias, clientType)
                .orElseGet(() -> serializer.newStoreKeysPair(UUID.randomUUID(), null, alias != null ? List.of(alias) : null, clientType));
        return createConnection(sessionStoreAndKeys);
    }

    /**
     * Creates a new connection using a six parts key representation
     *
     * @param sixParts the non-null six parts to use to create the connection
     * @return a non-null options selector
     */
    public T newConnection(SixPartsKeys sixParts) {
        var serialized = serializer.deserializeStoreKeysPair(null, sixParts.phoneNumber().number(), null, ClientType.MOBILE);
        if(serialized.isPresent()) {
            return createConnection(serialized.get());
        }

        var uuid = UUID.randomUUID();
        var keys = new KeysBuilder()
                .uuid(uuid)
                .phoneNumber(sixParts.phoneNumber())
                .noiseKeyPair(sixParts.noiseKeyPair())
                .identityKeyPair(sixParts.identityKeyPair())
                .identityId(sixParts.identityId())
                .registered(true)
                .clientType(ClientType.MOBILE)
                .build();
        keys.setSerializer(serializer);
        var phoneNumber = keys.phoneNumber()
                .map(PhoneNumber::number)
                .orElse(null);
        var store = Store.newStore(uuid, phoneNumber, null, ClientType.MOBILE);
        store.setSerializer(serializer);
        return createConnection(new StoreKeysPair(store, keys));
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
        return serializer.deserializeStoreKeysPair(sessionUuid, null, null, clientType)
                .map(this::createConnection);
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, an empty optional will be returned
     *
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(Long phoneNumber) {
        return serializer.deserializeStoreKeysPair(null, phoneNumber, null, clientType)
                .map(this::createConnection);
    }

    /**
     * Creates a new connection using an alias
     * If no connection is available, an empty optional will be returned
     *
     * @param alias the nullable alias to use to create the connection
     * @return a non-null options selector
     */
    public Optional<T> newOptionalConnection(String alias) {
        return serializer.deserializeStoreKeysPair(null, null, alias, clientType)
                .map(this::createConnection);
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
    private T createConnection(StoreKeysPair sessionStoreAndKeys) {
        return (T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
            case MOBILE -> new MobileOptionsBuilder(sessionStoreAndKeys.store(), sessionStoreAndKeys.keys());
        };
    }
}
