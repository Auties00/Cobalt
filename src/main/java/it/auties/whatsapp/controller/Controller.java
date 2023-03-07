package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers. It provides an easy
 * way to store IDs and serialize said class.
 */
@SuperBuilder
@Accessors(fluent = true)
@SuppressWarnings("unused")
public abstract sealed class Controller<T extends Controller<T>> implements JacksonProvider permits Store, Keys {
    /**
     * The serializer instance to use
     */
    @JsonIgnore
    protected ControllerSerializer serializer;

    /**
     * Returns the id of this controller
     *
     * @return an id
     */
    public abstract int id();

    /**
     * Serializes this object
     *
     * @param async whether the operation should be executed asynchronously
     */
    public abstract void serialize(boolean async);

    /**
     * Disposes this object
     */
    public abstract void dispose();

    /**
     * Sets the serializer of this controller
     *
     * @param serializer a serializer
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T serializer(ControllerSerializer serializer) {
        this.serializer = serializer;
        return (T) this;
    }

    /**
     * Converts this controller to a json. Useful when debugging.
     *
     * @return a non-null string
     */
    public String toJson() {
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot convert controller to json", exception);
        }
    }
}
