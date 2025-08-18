package it.auties.whatsapp.api;

import it.auties.whatsapp.api.MobileRegistrationBuilder.Unregistered;
import it.auties.whatsapp.api.MobileRegistrationBuilder.Unverified;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.util.Optional;

@SuppressWarnings("unused")
public final class MobileOptionsBuilder extends OptionsBuilder<MobileOptionsBuilder> {
    MobileOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    /**
     * Set the device to emulate
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder device(CompanionDevice device) {
        store.setDevice(device);
        return this;
    }

    /**
     * Sets the business' address
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessAddress(String businessAddress) {
        store.setBusinessAddress(businessAddress);
        return this;
    }

    /**
     * Sets the business' address longitude
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessLongitude(Double businessLongitude) {
        store.setBusinessLongitude(businessLongitude);
        return this;
    }

    /**
     * Sets the business' address latitude
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessLatitude(Double businessLatitude) {
        store.setBusinessLatitude(businessLatitude);
        return this;
    }

    /**
     * Sets the business' description
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessDescription(String businessDescription) {
        store.setBusinessDescription(businessDescription);
        return this;
    }

    /**
     * Sets the business' website
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessWebsite(String businessWebsite) {
        store.setBusinessWebsite(businessWebsite);
        return this;
    }

    /**
     * Sets the business' email
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessEmail(String businessEmail) {
        store.setBusinessEmail(businessEmail);
        return this;
    }

    /**
     * Sets the business' category
     *
     * @return the same instance for chaining
     */
    public MobileOptionsBuilder businessCategory(BusinessCategory businessCategory) {
        store.setBusinessCategory(businessCategory);
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
        if (!keys.registered()) {
            return Optional.empty();
        }

        return Optional.of(Whatsapp.customBuilder()
                .store(store)
                .keys(keys)
                .errorHandler(errorHandler)
                .build());
    }

    /**
     * Expects the session to still need verification
     * This means that you already have a code, but it hasn't already been sent to Whatsapp
     *
     * @return a non-null selector
     */
    public Unverified unverified() {
        return new Unverified(store, keys, errorHandler, null);
    }

    /**
     * Expects the session to still need registration
     * This means that you may or may not have a verification code, but that it hasn't already been sent to Whatsapp
     *
     * @return a non-null selector
     */
    public Unregistered unregistered() {
        return new Unregistered(store, keys, errorHandler);
    }
}
