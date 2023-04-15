package it.auties.whatsapp.api;

import it.auties.whatsapp.api.MobileRegistrationBuilder.Unregistered;
import it.auties.whatsapp.api.MobileRegistrationBuilder.Unverified;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.util.Validate;

import java.util.UUID;

@SuppressWarnings("unused")
public final class MobileOptionsBuilder extends OptionsBuilder<MobileOptionsBuilder> {
    public MobileOptionsBuilder(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType) {
        super(connectionUuid, serializer, connectionType, ClientType.APP_CLIENT);
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
