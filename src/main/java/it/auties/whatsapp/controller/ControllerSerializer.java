package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;

/**
 * This interface provides a standardized way to serialize a session
 */
@SuppressWarnings("unused")
public interface ControllerSerializer {
    /**
     * Returns the default serializer
     * This implementation uses .proto files compressed using gzip
     *
     * @return a serializer
     */
    static ControllerSerializer discarding() {
        return DiscardingControllerSerializer.instance();
    }

    /**
     * Returns the default serializer
     * This implementation uses .proto files compressed using gzip
     *
     * @return a serializer
     */
    static ControllerSerializer toProtobuf() {
        return new ProtobufControllerSerializer();
    }

    /**
     * Returns the default serializer
     * This implementation uses .proto files compressed using gzip
     *
     * @param baseDirectory the directory where all the sessions should be saved
     * @return a serializer
     */
    static ControllerSerializer toProtobuf(Path baseDirectory) {
        return new ProtobufControllerSerializer(baseDirectory);
    }

    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<UUID> listIds(WhatsappClientType type);

    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<PhoneNumber> listPhoneNumbers(WhatsappClientType type);

    /**
     * Creates a fresh pair of store and keys
     *
     * @param uuid        the non-null uuid
     * @param phoneNumber the nullable phone number
     * @param alias       the nullable alias
     * @param clientType  the non-null client type
     * @return a non-null store-keys pair
     */
    default StoreKeysPair newStoreKeysPair(UUID uuid, PhoneNumber phoneNumber, Collection<String> alias, WhatsappClientType clientType) {
        var store = Store.of(uuid, phoneNumber, alias, clientType);
        store.setSerializer(this);
        linkMetadata(store);
        var keys = Keys.of(uuid, phoneNumber, alias, clientType);
        keys.setSerializer(this);
        serializeKeys(keys);
        return new StoreKeysPair(store, keys);
    }

    /**
     * Deserializes a store-keys pair from a list of possible identifiers
     *
     * @param uuid        the nullable identifying unique id
     * @param phoneNumber the nullable identifying phone number
     * @param alias       the nullable identifying alias
     * @param clientType  the non-null client type
     * @return an optional store-keys pair
     */
    default Optional<StoreKeysPair> deserializeStoreKeysPair(UUID uuid, PhoneNumber phoneNumber, String alias, WhatsappClientType clientType) {
        if (uuid != null) {
            var store = deserializeStore(clientType, uuid);
            if(store.isEmpty()) {
                return Optional.empty();
            }

            store.get().setSerializer(this);
            attributeStore(store.get());
            var keys = deserializeKeys(clientType, uuid);
            if(keys.isEmpty()) {
                return Optional.empty();
            }

            keys.get().setSerializer(this);
            return Optional.of(new StoreKeysPair(store.get(), keys.get()));
        }

        if (phoneNumber != null) {
            var store = deserializeStore(clientType, phoneNumber);
            if(store.isEmpty()) {
                return Optional.empty();
            }

            store.get().setSerializer(this);
            attributeStore(store.get());
            var keys = deserializeKeys(clientType, phoneNumber);
            if(keys.isEmpty()) {
                return Optional.empty();
            }

            keys.get().setSerializer(this);
            return Optional.of(new StoreKeysPair(store.get(), keys.get()));
        }

        if (alias != null) {
            var store = deserializeStore(clientType, alias);
            if(store.isEmpty()) {
                return Optional.empty();
            }

            store.get().setSerializer(this);
            attributeStore(store.get());
            var keys = deserializeKeys(clientType,  alias);
            if(keys.isEmpty()) {
                return Optional.empty();
            }

            keys.get().setSerializer(this);
            return Optional.of(new StoreKeysPair(store.get(), keys.get()));
        }

        return Optional.empty();
    }

    /**
     * Serializes the keys
     *
     * @param keys  the non-null keys to serialize
     */
    void serializeKeys(Keys keys);

    /**
     * Serializes the store
     *
     * @param store the non-null store to serialize
     */
    void serializeStore(Store store);

    /**
     * Serializes the keys
     *
     * @param type the non-null type of client
     * @param id   the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(WhatsappClientType type, UUID id);

    /**
     * Serializes the keys
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(WhatsappClientType type, PhoneNumber phoneNumber);

    /**
     * Serializes the keys
     *
     * @param type  the non-null type of client
     * @param alias the alias number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(WhatsappClientType type, String alias);

    /**
     * Serializes the store
     *
     * @param type the non-null type of client
     * @param id   the id of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(WhatsappClientType type, UUID id);

    /**
     * Serializes the store
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(WhatsappClientType type, PhoneNumber phoneNumber);

    /**
     * Serializes the store
     *
     * @param type  the non-null type of client
     * @param alias the alias of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(WhatsappClientType type, String alias);

    /**
     * Deletes a session
     *
     * @param controller the non-null controller
     */
    void deleteSession(Controller controller);

    /**
     * Creates a link between the session and its metadata, usually phone number and alias
     *
     * @param controller a non-null controller
     */
    void linkMetadata(Controller controller);

    /**
     * Attributes the store asynchronously. This method is optionally used to load asynchronously
     * heavy data such as chats while the socket is connecting. If implemented, cache the returning
     * newsletters because the method may be called multiple times.
     *
     * @param store the non-null store to attribute
     */
    void attributeStore(Store store);
}