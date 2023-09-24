package it.auties.whatsapp.api;

import it.auties.whatsapp.api.MobileRegistrationBuilder.Unregistered;
import it.auties.whatsapp.api.MobileRegistrationBuilder.Unverified;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.KeysBuilder;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.controller.StoreBuilder;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.companion.CompanionDevice;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class MobileOptionsBuilder extends OptionsBuilder<MobileOptionsBuilder> {
    MobileOptionsBuilder(StoreBuilder store, KeysBuilder keys) {
        super(store, keys);
    }

    MobileOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    /**
     * Set the device to emulate
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder device(@NonNull CompanionDevice device){
        if (storeBuilder == null) {
            store.setDevice(device);
        } else {
            storeBuilder.device(device);
        }
        return this;
    }

    /**
     * Sets whether the registered account is a business account
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder business(boolean business){
        if (storeBuilder == null) {
            store.setBusiness(business);
        } else {
            storeBuilder.business(business);
        }
        return this;
    }

    /**
     * Sets the business' address
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessAddress(String businessAddress) {
        if (storeBuilder == null) {
            store.setBusinessAddress(businessAddress);
        } else {
            storeBuilder.businessAddress(businessAddress);
        }
        return this;
    }

    /**
     * Sets the business' address longitude
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessLongitude(Double businessLongitude) {
        if (storeBuilder == null) {
            store.setBusinessLongitude(businessLongitude);
        } else {
            storeBuilder.businessLongitude(businessLongitude);
        }
        return this;
    }

    /**
     * Sets the business' address latitude
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessLatitude(Double businessLatitude) {
        if (storeBuilder == null) {
            store.setBusinessLatitude(businessLatitude);
        } else {
            storeBuilder.businessLatitude(businessLatitude);
        }
        return this;
    }

    /**
     * Sets the business' description
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessDescription(String businessDescription) {
        if (storeBuilder == null) {
            store.setBusinessDescription(businessDescription);
        } else {
            storeBuilder.businessDescription(businessDescription);
        }
        return this;
    }

    /**
     * Sets the business' website
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessWebsite(String businessWebsite) {
        if (storeBuilder == null) {
            store.setBusinessWebsite(businessWebsite);
        } else {
            storeBuilder.businessWebsite(businessWebsite);
        }
        return this;
    }

    /**
     * Sets the business' email
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessEmail(String businessEmail) {
        if (storeBuilder == null) {
            store.setBusinessEmail(businessEmail);
        } else {
            storeBuilder.businessEmail(businessEmail);
        }
        return this;
    }

    /**
     * Sets the business' category
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessCategory(BusinessCategory businessCategory) {
        if (storeBuilder == null) {
            store.setBusinessCategory(businessCategory);
        } else {
            storeBuilder.businessCategory(businessCategory);
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
        var keys = Objects.requireNonNullElseGet(this.keys, keysBuilder::build);
        if(!keys.registered()){
            return Optional.empty();
        }

        return Optional.of(Whatsapp.customBuilder()
                .store(Objects.requireNonNullElseGet(store, storeBuilder::build))
                .keys(keys)
                .errorHandler(errorHandler)
                .socketExecutor(socketExecutor)
                .build());
    }

    /**
     * Expects the session to still need registration
     * This means that you may or may not have a verification code, but that it hasn't already been sent to Whatsapp
     *
     * @return a non-null selector
     */
    public Unregistered unregistered() {
        var store = Objects.requireNonNullElseGet(this.store, storeBuilder::build);
        var keys = Objects.requireNonNullElseGet(this.keys, keysBuilder::build);
        return new Unregistered(store, keys, errorHandler, socketExecutor);
    }

    /**
     * Expects the session to still need verification
     * This means that you already have a code, but it hasn't already been sent to Whatsapp
     *
     * @return a non-null selector
     */
    public Unverified unverified() {
        var store = Objects.requireNonNullElseGet(this.store, storeBuilder::build);
        var keys = Objects.requireNonNullElseGet(this.keys, keysBuilder::build);
        return new Unverified(store, keys, errorHandler, socketExecutor);
    }
}
