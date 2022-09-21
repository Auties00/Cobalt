package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.auties.whatsapp.util.ControllerProviderLoader;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Preferences;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers.
 * It provides an easy way to store IDs and serialize said class.
 */
@SuppressWarnings("unused")
public sealed interface Controller<T extends Controller<T>> extends JacksonProvider permits Store, Keys {
    static void deleteFolder(int id){
        try {
            var folder = Preferences.home()
                    .resolve(String.valueOf(id));
            Files.deleteIfExists(folder);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot delete folder", exception);
        }
    }

    /**
     * Returns the id of this controller
     *
     * @return an id
     */
    int id();

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
     * Serializes this object
     */
    void serialize();

    /**
     * Set whether the default serializer should be used
     *
     * @return the same instance
     */
    T useDefaultSerializer(boolean useDefaultSerializer);

    /**
     * Converts this object to JSON
     *
     * @return a non-null string
     */
    default String toJSON() throws JsonProcessingException {
        return JSON.writeValueAsString(this);
    }
}
