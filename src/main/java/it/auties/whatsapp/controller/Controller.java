package it.auties.whatsapp.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.util.Json;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Optional;
import java.util.UUID;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers. It provides an easy
 * way to store IDs and serialize said class.
 */
@SuperBuilder
@Accessors(fluent = true)
@SuppressWarnings("unused")
public abstract sealed class Controller<T extends Controller<T>> permits Store, Keys {
    /**
     * The id of this controller
     */
    @NonNull
    @Getter
    protected UUID uuid;

    /**
     * The phone number of the associated companion
     */
    private PhoneNumber phoneNumber;

    /**
     * The serializer instance to use
     */
    @JsonIgnore
    protected ControllerSerializer serializer;

    /**
     * The client type
     */
    @Getter
    @NonNull
    protected ClientType clientType;

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
     * Returns the serializer
     *
     * @return a non-null serializer
     */
    public ControllerSerializer serializer() {
        return serializer;
    }

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
     * Returns the phone number of this controller
     *
     * @return an optional
     */
    public Optional<PhoneNumber> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Sets the phone number used by this session
     *
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T phoneNumber(@NonNull PhoneNumber phoneNumber){
        this.phoneNumber = phoneNumber;
        serializer.linkPhoneNumber(this);
        return (T) this;
    }

    /**
     * Converts this controller to a json. Useful when debugging.
     *
     * @return a non-null string
     */
    public String toJson() {
        return Json.writeValueAsString(this, true);
    }

    /**
     * Deletes the current session
     */
    public void deleteSession() {
        serializer.deleteSession(this);
    }
}
