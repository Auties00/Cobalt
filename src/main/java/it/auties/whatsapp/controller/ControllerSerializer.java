package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import lombok.NonNull;

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
     * Serializes the keys
     *
     * @param keys  the non-null keys to serialize
     * @param async whether the operation should be executed asynchronously
     */
    void serializeKeys(Keys keys, boolean async);

    /**
     * Serializes the store
     *
     * @param store the non-null store to serialize
     * @param async whether the operation should be executed asynchronously
     */
    void serializeStore(Store store, boolean async);

    /**
     * Deletes a session
     *
     * @param type the non-null type of client
     * @param id   the id of the session to delete
     */
    void deleteSession(ClientType type, UUID id);

    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<UUID> listIds(@NonNull ClientType type);

    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<PhoneNumber> listPhoneNumbers(@NonNull ClientType type);

    /**
     * Serializes the keys
     *
     * @param type the non-null type of client
     * @param id the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(@NonNull ClientType type, UUID id);

    /**
     * Serializes the keys
     *
     * @param type the non-null type of client
     * @param phoneNumber the phone number of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(@NonNull ClientType type, long phoneNumber);

    /**
     * Serializes the store
     *
     * @param type the non-null type of client
     * @param id the id of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(@NonNull ClientType type, UUID id);

    /**
     * Serializes the store
     *
     * @param type        the non-null type of client
     * @param phoneNumber the phone number of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(@NonNull ClientType type, long phoneNumber);

    /**
     * Creates a link between the session store and the phone number
     * This may not be implemented
     *
     * @param store a non-null store
     */
    default void linkPhoneNumber(@NonNull Store store) {

    }

    /**
     * Attributes the store asynchronously. This method is optionally used to load asynchronously
     * heavy data such as chats while the socket is connecting. If implemented, cache the returning
     * result because the method may be called multiple times.
     *
     * @param store the non-null store to attribute
     * @return a completable result
     */
    @SuppressWarnings("UnusedReturnValue")
    default CompletableFuture<Void> attributeStore(@SuppressWarnings("unused") Store store) {
        return CompletableFuture.completedFuture(null);
    }
}