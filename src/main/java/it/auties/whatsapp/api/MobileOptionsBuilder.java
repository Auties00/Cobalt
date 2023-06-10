package it.auties.whatsapp.api;

import it.auties.whatsapp.api.MobileRegistrationBuilder.Unregistered;
import it.auties.whatsapp.api.MobileRegistrationBuilder.Unverified;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.RegistrationStatus;
import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public final class MobileOptionsBuilder extends OptionsBuilder<MobileOptionsBuilder> {
    private MobileOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    static MobileOptionsBuilder of(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, ClientType.MOBILE);
        var store = Store.of(uuid, ClientType.MOBILE, serializer);
        var keys = Keys.of(uuid, ClientType.MOBILE, serializer);
        return new MobileOptionsBuilder(store, keys);
    }

    static Optional<MobileOptionsBuilder> ofNullable(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, ClientType.MOBILE);
        var store = Store.ofNullable(uuid, ClientType.MOBILE, serializer);
        var keys = Keys.ofNullable(uuid, ClientType.MOBILE, serializer);
        if(store.isEmpty() || keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new MobileOptionsBuilder(store.get(), keys.get()));
    }

    static MobileOptionsBuilder of(long phoneNumber, ControllerSerializer serializer){
        var uuid = UUID.randomUUID();
        var store = Store.of(uuid, phoneNumber, ClientType.MOBILE, serializer);
        var keys = Keys.of(uuid, phoneNumber, ClientType.MOBILE, serializer);
        return new MobileOptionsBuilder(store, keys);
    }

    static Optional<MobileOptionsBuilder> ofNullable(Long phoneNumber, ControllerSerializer serializer){
        var store = Store.ofNullable(phoneNumber, ClientType.MOBILE, serializer);
        var keys = Keys.ofNullable(phoneNumber, ClientType.MOBILE, serializer);
        if(store.isEmpty() || keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new MobileOptionsBuilder(store.get(), keys.get()));
    }


    static MobileOptionsBuilder of(String alias, ControllerSerializer serializer){
        var uuid = UUID.randomUUID();
        var store = Store.of(uuid, alias, ClientType.MOBILE, serializer);
        var keys = Keys.of(uuid, alias, ClientType.MOBILE, serializer);
        return new MobileOptionsBuilder(store, keys);
    }

    static Optional<MobileOptionsBuilder> ofNullable(String alias, ControllerSerializer serializer){
        var store = Store.ofNullable(alias, ClientType.MOBILE, serializer);
        var keys = Keys.ofNullable(alias, ClientType.MOBILE, serializer);
        if(store.isEmpty() || keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new MobileOptionsBuilder(store.get(), keys.get()));
    }


    /**
     * Set the device to emulate
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder device(@NonNull CompanionDevice device){
        if(store != null) {
            store.device(device);
        }
        return this;
    }

    /**
     * Sets whether the registered account is a business account
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
     * Sets the business' address
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessAddress(String businessAddress) {
        if(store != null) {
            store.businessAddress(businessAddress);
        }
        return this;
    }

    /**
     * Sets the business' address longitude
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessLongitude(Long businessLongitude) {
        if(store != null) {
            store.businessLongitude(businessLongitude);
        }
        return this;
    }

    /**
     * Sets the business' address latitude
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessLatitude(Long businessLatitude) {
        if(store != null) {
            store.businessLatitude(businessLatitude);
        }
        return this;
    }

    /**
     * Sets the business' description
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessDescription(String businessDescription) {
        if(store != null) {
            store.businessDescription(businessDescription);
        }
        return this;
    }

    /**
     * Sets the business' website
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessWebsite(String businessWebsite) {
        if(store != null) {
            store.businessWebsite(businessWebsite);
        }
        return this;
    }

    /**
     * Sets the business' email
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessEmail(String businessEmail) {
        if(store != null) {
            store.businessEmail(businessEmail);
        }
        return this;
    }

    /**
     * Sets the business' category
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessCategory(BusinessCategory businessCategory) {
        if(store != null) {
            store.businessCategory(businessCategory);
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

        return Optional.of(Whatsapp.of(store, keys));
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
