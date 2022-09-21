package it.auties.whatsapp.controller;

import java.security.Key;
import java.util.LinkedList;

/**
 * This interface provides a standardized way to serialize a session.
 * Implement this interface and <a href="https://www.baeldung.com/java-spi#3-service-provider">register it in your manifest</a>
 */
public interface ControllerProvider {
    /**
     * Returns all the known IDs
     *
     * @return a non-null list
     */
    LinkedList<Integer> ids();

    /**
     * Serializes the keys
     *
     * @param keys the non-null keys to serialize
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
}
