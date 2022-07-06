package it.auties.whatsapp.controller;

/**
 * This interface provides a standardized way to serialize a session.
 * Implement this interface and <a href="https://www.baeldung.com/java-spi#3-service-provider">register it in your manifest</a>
 */
public interface ControllerSerializer {
    /**
     * Serializes a controller
     *
     * @param controller the non-null controller to serialize
     * @param close      whether this method was called because the connection was closed, this means that async operations should not be used
     */
    void serialize(Controller<?> controller, boolean close);
}
