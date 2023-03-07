package it.auties.whatsapp.controller;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This interface provides a standardized way to deserialize a session
 */
public interface ControllerDeserializer {
    /**
     * Returns all the known IDs
     *
     * @return a non-null linked list
     */
    LinkedList<Integer> findIds();

    /**
     * Serializes the keys
     *
     * @param id the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(int id);

    /**
     * Serializes the store
     *
     * @param id the id of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(int id);

    /**
     * Attributes the store asynchronously. This method is optionally used to load asynchronously
     * heavy data such as chats while the socket is connecting. If implemented, cache the returning
     * result because the method may be called multiple times.
     *
     * @param store the non-null store to attribute
     * @return a completable result
     */
    default CompletableFuture<Void> attributeStore(@SuppressWarnings("unused") Store store) {
        return CompletableFuture.completedFuture(null);
    }
}
