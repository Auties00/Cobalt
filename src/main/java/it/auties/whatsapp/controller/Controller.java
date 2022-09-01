package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.util.ControllerProviderLoader;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Preferences;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers.
 * It provides an easy way to store IDs and serialize said class.
 */
@SuppressWarnings("unused")
public sealed interface Controller<T extends Controller<T>> extends JacksonProvider permits Store, Keys {
    /**
     * Converts this object to JSON
     *
     * @return a non-null string
     */
    default String toJSON() throws JsonProcessingException {
        return JSON.writeValueAsString(this);
    }

    /**
     * Serializes this object
     */
    default void serialize() {
        ControllerProviderLoader.providers(useDefaultSerializer())
                .forEach(serializer -> serializer.serialize(this));
    }

    /**
     * Deletes this object from memory
     */
    default void delete() {
        preferences().delete();
    }

    /**
     * Returns the id of this controller
     *
     * @return an id
     */
    int id();

    /**
     * Returns the preferences for this object
     *
     * @return a non-null preferences object
     */
    Preferences preferences();

    /**
     * Clears some or all fields of this object
     */
    void clear();

    /**
     * Disposes this object
     */
    void dispose();

    /**
     * Whether the default serializer should be used
     *
     * @return a boolean
     */
    boolean useDefaultSerializer();

    /**
     * Set whether the default serializer should be used
     */
    T useDefaultSerializer(boolean useDefaultSerializer);
}
