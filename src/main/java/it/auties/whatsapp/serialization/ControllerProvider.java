package it.auties.whatsapp.serialization;

import java.util.LinkedList;

/**
 * This interface provides a standardized way to list all sessions.
 * Implement this interface and <a href="https://www.baeldung.com/java-spi#3-service-provider">register it in your manifest</a>
 */
public sealed interface ControllerProvider permits ControllerDeserializerProvider, ControllerSerializerProvider {
    /**
     * Returns all the known IDs
     *
     * @return a non-null linked list
     */
    LinkedList<Integer> findIds();
}
