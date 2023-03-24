package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import lombok.NonNull;

import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This interface provides a standardized way to deserialize a session
 */
public interface ControllerDeserializer {
    /**
     * Returns all the known IDs
     *
     * @param type the non-null type of client
     * @return a non-null linked list
     */
    LinkedList<UUID> findIds(@NonNull ClientType type);

    /**
     * Serializes the keys
     *
     * @param type the non-null type of client
     * @param id the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(@NonNull ClientType type, UUID id);

    /**
     * Serializes the store
     *
     * @param type the non-null type of client
     * @param id the id of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(@NonNull ClientType type, UUID id);

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
