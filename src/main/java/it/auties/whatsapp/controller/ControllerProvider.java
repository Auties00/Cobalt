package it.auties.whatsapp.controller;

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
     * Serializes a controller
     *
     * @param controller the non-null controller to serialize
     */
    void serialize(Controller<?> controller);
}
