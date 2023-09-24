package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.KeysBuilder;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.controller.StoreBuilder;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class WebOptionsBuilder extends OptionsBuilder<WebOptionsBuilder> {
    private Whatsapp whatsapp;
    WebOptionsBuilder(StoreBuilder storeBuilder, KeysBuilder keysBuilder) {
        super(storeBuilder, keysBuilder);
    }

    WebOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    /**
     * Sets how much chat history Whatsapp should send when the QR is first scanned.
     * By default, one year
     *
     * @return the same instance for chaining
     */
    public WebOptionsBuilder historyLength(@NonNull WebHistoryLength historyLength) {
        if(store != null) {
            store.setHistoryLength(historyLength);
        }else {
            storeBuilder.historyLength(historyLength);
        }
        return this;
    }

    /**
     * Creates a Whatsapp instance with a qr handler
     *
     * @param qrHandler the non-null handler to use
     * @return a Whatsapp instance
     */
    public Whatsapp unregistered(@NonNull QrHandler qrHandler) {
        if (whatsapp == null) {
            this.whatsapp = Whatsapp.customBuilder()
                    .store(Objects.requireNonNullElseGet(store, storeBuilder::build))
                    .keys(Objects.requireNonNullElseGet(keys, keysBuilder::build))
                    .errorHandler(errorHandler)
                    .webVerificationSupport(qrHandler)
                    .socketExecutor(socketExecutor)
                    .build();
        }

        return whatsapp;
    }

    /**
     * Creates a Whatsapp instance with an OTP handler
     *
     * @param phoneNumber        the phone number of the user
     * @param pairingCodeHandler the non-null handler for the pairing code
     * @return a Whatsapp instance
     */
    public Whatsapp unregistered(long phoneNumber, @NonNull PairingCodeHandler pairingCodeHandler) {
        if (whatsapp == null) {
            if(store != null) {
                store.setPhoneNumber(PhoneNumber.of(phoneNumber));
            }else {
                storeBuilder.phoneNumber(PhoneNumber.of(phoneNumber));
            }

            this.whatsapp = Whatsapp.customBuilder()
                    .store(Objects.requireNonNullElseGet(store, storeBuilder::build))
                    .keys(Objects.requireNonNullElseGet(keys, keysBuilder::build))
                    .errorHandler(errorHandler)
                    .webVerificationSupport(pairingCodeHandler)
                    .socketExecutor(socketExecutor)
                    .build();
        }

        return whatsapp;
    }

    /**
     * Creates a Whatsapp instance with no handlers
     * This method assumes that you have already logged in using a QR code or OTP
     * Otherwise, it returns an empty optional.
     *
     * @return an optional
     */
    public Optional<Whatsapp> registered() {
        var keys = Objects.requireNonNullElseGet(this.keys, keysBuilder::build);
        if(!keys.registered()){
            return Optional.empty();
        }

        if (whatsapp == null) {
            this.whatsapp = Whatsapp.customBuilder()
                    .store(Objects.requireNonNullElseGet(store, storeBuilder::build))
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .socketExecutor(socketExecutor)
                    .build();
        }

        return Optional.of(whatsapp);
    }
}