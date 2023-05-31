package it.auties.whatsapp.api;

import it.auties.whatsapp.api.MobileRegistrationBuilder.Unregistered;
import it.auties.whatsapp.api.MobileRegistrationBuilder.Unverified;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.RegistrationStatus;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public final class MobileOptionsBuilder extends OptionsBuilder<MobileOptionsBuilder> {
    MobileOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    static Optional<MobileOptionsBuilder> of(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, ClientType.MOBILE);
        var required = connectionType == ConnectionType.KNOWN;
        var store = Store.of(uuid, null, ClientType.MOBILE, serializer, required);
        if(required && store.isEmpty()){
            return Optional.empty();
        }

        var keys = Keys.of(uuid, null, ClientType.MOBILE, serializer, required);
        if(required && keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new MobileOptionsBuilder(store.get(), keys.get()));
    }

    static Optional<MobileOptionsBuilder> of(long phoneNumber, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(null, serializer, connectionType, ClientType.MOBILE);
        var required = connectionType == ConnectionType.KNOWN;
        var store = Store.of(uuid, null, ClientType.MOBILE, serializer, required);
        if(required && store.isEmpty()){
            return Optional.empty();
        }

        var keys = Keys.of(uuid, null, ClientType.MOBILE, serializer, required);
        if(required && keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new MobileOptionsBuilder(store.get(), keys.get()));
    }

    /**
     * Set the operating system of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder osType(@NonNull UserAgentPlatform osType){
        if(store != null) {
            store.os(osType);
        }
        return this;
    }

    /**
     * Set the operating system's version of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder osVersion(@NonNull String osVersion){
        if(store != null) {
            store.osVersion(osVersion);
        }
        return this;
    }

    /**
     * Set the model of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder model(@NonNull String model){
        if(store != null) {
            store.model(model);
        }
        return this;
    }

    /**
     * Set the manufacturer of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder manufacturer(@NonNull String manufacturer){
        if(store != null) {
            store.manufacturer(manufacturer);
        }
        return this;
    }

    /**
     * Set whether the registered account is a business account
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder business(boolean business){
        if(store != null) {
            store.business(business);
        }
        return this;
    }

    /**
     * Expects the session to be already registered
     * This means that the verification code has already been sent to Whatsapp
     * If this is not the case, an exception will be thrown
     *
     * @return a non-null optional of whatsapp
     */
    public Optional<Whatsapp> registered() {
        if(keys.registrationStatus() == RegistrationStatus.UNREGISTERED){
            return Optional.empty();
        }

        return Optional.of(new Whatsapp(store, keys));
    }

    /**
     * Expects the session to still need registration
     * This means that you may or may not have a verification code, but that it hasn't already been sent to Whatsapp
     *
     * @return a non-null selector
     */
    public Unregistered unregistered() {
        return new Unregistered(store, keys);
    }

    /**
     * Expects the session to still need verification
     * This means that you already have a code, but it hasn't already been sent to Whatsapp
     *
     * @return a non-null selector
     */
    public Unverified unverified() {
        return new Unverified(store, keys);
    }
}
