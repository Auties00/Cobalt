package it.auties.whatsapp.api;

import it.auties.whatsapp.api.MobileRegistrationBuilder.Unregistered;
import it.auties.whatsapp.api.MobileRegistrationBuilder.Unverified;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.util.UUID;

@SuppressWarnings("unused")
public final class MobileOptionsBuilder extends OptionsBuilder<MobileOptionsBuilder> {
    public MobileOptionsBuilder(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType) {
        super(connectionUuid, serializer, connectionType, ClientType.APP_CLIENT);
    }

    public MobileOptionsBuilder(long phoneNumber, ControllerSerializer serializer, ConnectionType connectionType) {
        super(phoneNumber, serializer, connectionType, ClientType.WEB_CLIENT);
    }


    /**
     * Set the operating system of the associated companion
     *
     * @return the same instance for chaining
     */
    private MobileOptionsBuilder osType(@NonNull UserAgentPlatform osType){
        store.osType(osType);
        return this;
    }

    /**
     * Set the operating system's version of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder osVersion(@NonNull String osVersion){
        store.osVersion(osVersion);
        return this;
    }

    /**
     * Set the model of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder model(@NonNull String model){
        store.model(model);
        return this;
    }

    /**
     * Set the manufacturer of the associated companion
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder manufacturer(@NonNull String manufacturer){
        store.manufacturer(manufacturer);
        return this;
    }

    /**
     * Set whether the registered account is a business account
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder business(boolean business){
        store.business(business);
        return this;
    }

    /**
     * Expects the session to be already registered
     * This means that the verification code has already been sent to Whatsapp
     * If this is not the case, an exception will be thrown
     *
     * @throws IllegalStateException if the session is not registered
     * @return a non-null selector
     */
    public Whatsapp registered() {
        Validate.isTrue(keys.registered(), "Expected session to be already registered", IllegalStateException.class);
        return new Whatsapp(store, keys);
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
