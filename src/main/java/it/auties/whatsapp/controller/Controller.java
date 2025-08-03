package it.auties.whatsapp.controller;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;

import java.util.*;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers. It provides an easy
 * way to store IDs and serialize said class.
 */
@SuppressWarnings("unused")
@ProtobufMessage
public abstract sealed class Controller permits Store, Keys {
    /**
     * The id of this controller
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    protected final UUID uuid;

    /**
     * The phone number of the associated companion
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    private PhoneNumber phoneNumber;

    /**
     * The serializer instance to use
     */
    protected ControllerSerializer serializer;

    /**
     * The client type
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    protected final WhatsappClientType clientType;

    /**
     * A list of alias for the controller, can be used in place of UUID
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    protected final Collection<String> alias;

    public Controller(UUID uuid, PhoneNumber phoneNumber, ControllerSerializer serializer, WhatsappClientType clientType, Collection<String> alias) {
        this.uuid = Objects.requireNonNull(uuid, "Missing uuid");
        this.phoneNumber = phoneNumber;
        this.serializer = serializer;
        this.clientType = clientType;
        this.alias = Objects.requireNonNullElseGet(alias, ArrayList::new);
    }

    /**
     * Serializes this object
     */
    public abstract void serialize();

    /**
     * Disposes this object
     */
    public abstract void dispose();

    public UUID uuid() {
        return uuid;
    }

    public WhatsappClientType clientType() {
        return this.clientType;
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
     */
    public void setPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

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
     */
    public void setSerializer(ControllerSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Returns an immutable collection of alias
     *
     * @return an immutable collection
     */
    public Collection<String> alias() {
        return Collections.unmodifiableCollection(alias);
    }

    /**
     * Adds an alias to this controller
     *
     * @param entry the non-null alias to add
     */
    public void addAlias(String entry) {
        alias.add(entry);
    }

    /**
     * Removes an alias to this controller
     *
     * @param entry the non-null alias to remove
     */
    public void removeAlias(String entry) {
        alias.remove(entry);
    }

    /**
     * Removes all alias from this controller
     */
    public void removeAlias() {
        alias.clear();
    }

    /**
     * Deletes the current session
     */
    public void deleteSession() {
        serializer.deleteSession(this);
    }
}
