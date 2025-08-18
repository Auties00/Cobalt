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
        serializeStore(store);
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
    @SuppressWarnings("DuplicatedCode")
    default Optional<StoreKeysPair> deserializeStoreKeysPair(UUID uuid, PhoneNumber phoneNumber, String alias, WhatsappClientType clientType) {
        if (uuid != null) {
            var store = deserializeStore(clientType, uuid)
                    .orElse(null);
            var keys = deserializeKeys(clientType, uuid)
                    .orElse(null);
            var result = createStoreKeysPair(store, keys);
            if(result.isPresent()) {
                return result;
            }
        }

        if (phoneNumber != null) {
            var store = deserializeStore(clientType, phoneNumber)
                    .orElse(null);
            var keys = deserializeKeys(clientType, phoneNumber)
                    .orElse(null);
            var result = createStoreKeysPair(store, keys);
            if(result.isPresent()) {
                return result;
            }
        }

        if (alias != null) {
            var store = deserializeStore(clientType, alias)
                    .orElse(null);
            var keys = deserializeKeys(clientType, alias)
                    .orElse(null);
            var result = createStoreKeysPair(store, keys);
            if(result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    private Optional<StoreKeysPair> createStoreKeysPair(Store store, Keys keys) {
        if (store == null || keys == null) {
            return Optional.empty();
        }
        store.setSerializer(this);
        keys.setSerializer(this);
        var result = new StoreKeysPair(store, keys);
        return Optional.of(result);
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
     * Deserializes the keys
     *
     * @param type the non-null type of client
     * @param id   the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(WhatsappClientType type, UUID id);

    /**
     * Deserializes the keys
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(WhatsappClientType type, PhoneNumber phoneNumber);

    /**
     * Deserializes the keys
     *
     * @param type  the non-null type of client
     * @param alias the alias number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(WhatsappClientType type, String alias);

    /**
     * Deserializes the store
     * This method should only block to deserialize the data related to the store that is strictly necessary to boostrap a session
     * Everything else, like the chats and newsletters, should be deserialized in a non-blocking fashion
     *
     * @param type the non-null type of client
     * @param id   the id of the store
     * @return a non-null store
     * @see ControllerSerializer#finishDeserializeStore(Store)
     */
    Optional<Store> deserializeStore(WhatsappClientType type, UUID id);

    /**
     * Deserializes the store
     * This method should only block to deserialize the data related to the store that is strictly necessary to boostrap a session
     * Everything else, like the chats and newsletters, should be deserialized in a non-blocking fashion
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the store
     * @return a non-null store
     * @see ControllerSerializer#finishDeserializeStore(Store)
     */
    Optional<Store> deserializeStore(WhatsappClientType type, PhoneNumber phoneNumber);

    /**
     * Deserializes the store
     * This method should only block to deserialize the data related to the store that is strictly necessary to boostrap a session
     * Everything else, like the chats and newsletters, should be deserialized in a non-blocking fashion
     *
     * @param type  the non-null type of client
     * @param alias the alias of the store
     * @return a non-null store
     * @see ControllerSerializer#finishDeserializeStore(Store)
     */
    Optional<Store> deserializeStore(WhatsappClientType type, String alias);

    /**
     * Deletes a session
     *
     * @param controller the non-null controller
     */
    void deleteSession(Controller controller);

    /**
     * Waits for the store to be done deserializing
     *
     * @param store a non-null store
     */
    void finishDeserializeStore(Store store);
}