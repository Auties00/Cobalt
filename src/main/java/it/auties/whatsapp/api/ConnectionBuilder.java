package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.DefaultControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.PhoneNumber;

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
    private CompanionDevice device;

    ConnectionBuilder(ClientType clientType) {
        this.clientType = clientType;
        this.serializer = DefaultControllerSerializer.instance();
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
     * Sets the device to use
     *
     * @param device the non-null device to use
     * @return the same instance for chaining
     */
    public ConnectionBuilder<T> device(CompanionDevice device) {
        this.device = device;
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
    @SuppressWarnings("unchecked")
    public T newConnection(UUID uuid) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        var store = Store.builder()
                .uuid(uuid)
                .clientType(clientType)
                .device(device)
                .serializer(serializer);
        var keys = Keys.builder()
                .uuid(uuid)
                .clientType(clientType)
                .serializer(serializer);
        return (T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(store, keys);
            case MOBILE -> new MobileOptionsBuilder(store, keys);
        };
    }

    /**
     * Creates a new connection using a phone number
     * If a session with the given phone number already exists, it will be retrieved.
     * Otherwise, a new one will be created.
     *
     * @param phoneNumber the nullable uuid to use to create the connection
     * @return a non-null options selector
     */
    @SuppressWarnings("unchecked")
    public T newConnection(long phoneNumber) {
        var uuid = UUID.randomUUID();
        var store = Store.builder()
                .uuid(uuid)
                .clientType(clientType)
                .device(device)
                .phoneNumber(PhoneNumber.of(phoneNumber))
                .serializer(serializer);
        var keys = Keys.builder()
                .uuid(uuid)
                .clientType(clientType)
                .phoneNumber(PhoneNumber.of(phoneNumber))
                .serializer(serializer);
        return (T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(store, keys);
            case MOBILE -> new MobileOptionsBuilder(store, keys);
        };
    }

    /**
     * Creates a new connection using an alias
     * If a session with the given alias already exists, it will be retrieved.
     * Otherwise, a new one will be created.
     *
     * @param alias the nullable alias to use to create the connection
     * @return a non-null options selector
     */
    @SuppressWarnings("unchecked")
    public T newConnection(String alias) {
        var uuid = UUID.randomUUID();
        var store = Store.builder()
                .uuid(uuid)
                .clientType(clientType)
                .device(device)
                .serializer(serializer)
                .alias(alias);
        var keys = Keys.builder()
                .uuid(uuid)
                .clientType(clientType)
                .serializer(serializer)
                .alias(alias);
        return (T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(store, keys);
            case MOBILE -> new MobileOptionsBuilder(store, keys);
        };
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
    @SuppressWarnings({"unchecked", "OptionalIsPresent"})
    public Optional<T> newOptionalConnection(UUID uuid) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        var store = Store.builder()
                .uuid(uuid)
                .clientType(clientType)
                .device(device)
                .serializer(serializer)
                .deserialize();
        if (store.isEmpty()) {
            return Optional.empty();
        }

        var keys = Keys.builder()
                .uuid(uuid)
                .clientType(clientType)
                .serializer(serializer)
                .deserialize();
        if (keys.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(store.get(), keys.get());
            case MOBILE -> new MobileOptionsBuilder(store.get(), keys.get());
        });
    }

    /**
     * Creates a new connection from the last connection that was serialized
     * If no connection is available, an empty optional will be returned
     *
     * @return a non-null options selector
     */
    @SuppressWarnings({"unchecked", "OptionalIsPresent"})
    public Optional<T> newOptionalConnection(Long phoneNumber) {
        var uuid = UUID.randomUUID();
        var store = Store.builder()
                .uuid(uuid)
                .clientType(clientType)
                .device(device)
                .phoneNumber(PhoneNumber.ofNullable(phoneNumber).orElse(null))
                .serializer(serializer)
                .deserialize();
        if (store.isEmpty()) {
            return Optional.empty();
        }

        var keys = Keys.builder()
                .uuid(uuid)
                .clientType(clientType)
                .phoneNumber(PhoneNumber.ofNullable(phoneNumber).orElse(null))
                .serializer(serializer)
                .deserialize();
        if (keys.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(store.get(), keys.get());
            case MOBILE -> new MobileOptionsBuilder(store.get(), keys.get());
        });
    }

    /**
     * Creates a new connection using an alias
     * If no connection is available, an empty optional will be returned
     *
     * @param alias the nullable alias to use to create the connection
     * @return a non-null options selector
     */
    @SuppressWarnings({"unchecked", "OptionalIsPresent"})
    public Optional<T> newOptionalConnection(String alias) {
        var uuid = UUID.randomUUID();
        var store = Store.builder()
                .uuid(uuid)
                .clientType(clientType)
                .device(device)
                .serializer(serializer)
                .alias(alias)
                .deserialize();
        if (store.isEmpty()) {
            return Optional.empty();
        }

        var keys = Keys.builder()
                .uuid(uuid)
                .clientType(clientType)
                .serializer(serializer)
                .alias(alias)
                .deserialize();
        if (keys.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((T) switch (clientType) {
            case WEB -> new WebOptionsBuilder(store.get(), keys.get());
            case MOBILE -> new MobileOptionsBuilder(store.get(), keys.get());
        });
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
}
