package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.util.DefaultControllerSerializer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    static ControllerSerializer toProtobuf() {
        return DefaultControllerSerializer.of();
    }

    /**
     * Returns the default serializer
     * This implementation uses .proto files compressed using gzip
     *
     * @param baseDirectory the directory where all the sessions should be saved
     * @return a serializer
     */
    static ControllerSerializer toProtobuf(Path baseDirectory) {
        return DefaultControllerSerializer.of(baseDirectory);
    }

    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<UUID> listIds(ClientType type);

    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<PhoneNumber> listPhoneNumbers(ClientType type);

    /**
     * Creates a fresh pair of store and keys
     *
     * @param uuid        the non-null uuid
     * @param phoneNumber the nullable phone number
     * @param alias       the nullable alias
     * @param clientType  the non-null client type
     * @return a non-null store-keys pair
     */
    StoreKeysPair newStoreKeysPair(UUID uuid, Long phoneNumber, Collection<String> alias, ClientType clientType);

    /**
     * Deserializes a store-keys pair from a list of possible identifiers
     *
     * @param uuid        the nullable identifying unique id
     * @param phoneNumber the nullable identifying phone number
     * @param alias       the nullable identifying alias
     * @param clientType  the non-null client type
     * @return an optional store-keys pair
     */
    Optional<StoreKeysPair> deserializeStoreKeysPair(UUID uuid, Long phoneNumber, String alias, ClientType clientType);

    /**
     * Serializes the keys
     *
     * @param keys  the non-null keys to serialize
     * @param async whether the operation should be executed asynchronously
     */
    CompletableFuture<Void> serializeKeys(Keys keys, boolean async);

    /**
     * Serializes the store
     *
     * @param store the non-null store to serialize
     * @param async whether the operation should be executed asynchronously
     */
    CompletableFuture<Void> serializeStore(Store store, boolean async);

    /**
     * Serializes the keys
     *
     * @param type the non-null type of client
     * @param id   the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(ClientType type, UUID id);

    /**
     * Serializes the keys
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(ClientType type, long phoneNumber);


    /**
     * Serializes the keys
     *
     * @param type  the non-null type of client
     * @param alias the alias number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(ClientType type, String alias);

    /**
     * Serializes the store
     *
     * @param type the non-null type of client
     * @param id   the id of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(ClientType type, UUID id);

    /**
     * Serializes the store
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(ClientType type, long phoneNumber);

    /**
     * Serializes the store
     *
     * @param type  the non-null type of client
     * @param alias the alias of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(ClientType type, String alias);

    /**
     * Deletes a session
     *
     * @param controller the non-null controller
     */
    void deleteSession(Controller<?> controller);

    /**
     * Creates a link between the session and its metadata, usually phone number and alias
     *
     * @param controller a non-null controller
     */
    default void linkMetadata(Controller<?> controller) {

    }

    /**
     * Attributes the store asynchronously. This method is optionally used to load asynchronously
     * heavy data such as chats while the socket is connecting. If implemented, cache the returning
     * newsletters because the method may be called multiple times.
     *
     * @param store the non-null store to attribute
     * @return a completable newsletters
     */
    default CompletableFuture<Void> attributeStore(Store store) {
        return CompletableFuture.completedFuture(null);
    }
}