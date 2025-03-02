package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.PhoneNumber;

import java.util.Optional;

@SuppressWarnings("unused")
public final class WebOptionsBuilder extends OptionsBuilder<WebOptionsBuilder> {
    private Whatsapp whatsapp;

    WebOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    /**
     * Whether the library should send receipts automatically for messages
     * By default disabled
     * For the web api, if enabled, the companion won't receive notifications
     *
     * @return the same instance for chaining
     */
    public WebOptionsBuilder automaticMessageReceipts(boolean automaticMessageReceipts) {
        store.setAutomaticMessageReceipts(automaticMessageReceipts);
        return this;
    }

    /**
     * Sets how much chat history Whatsapp should send when the QR is first scanned.
     * By default, one year
     *
     * @return the same instance for chaining
     */
    public WebOptionsBuilder historySetting(WebHistorySetting historyLength) {
        store.setWebHistorySetting(historyLength);
        return this;
    }

    /**
     * Creates a Whatsapp instance with a qr handler
     *
     * @param qrHandler the non-null handler to use
     * @return a Whatsapp instance
     */
    public Whatsapp unregistered(QrHandler qrHandler) {
        if (whatsapp == null) {
            this.whatsapp = Whatsapp.customBuilder()
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .webVerificationSupport(qrHandler)
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
    public Whatsapp unregistered(long phoneNumber, PairingCodeHandler pairingCodeHandler) {
        if (whatsapp == null) {
            store.setPhoneNumber(PhoneNumber.of(phoneNumber));
            this.whatsapp = Whatsapp.customBuilder()
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .webVerificationSupport(pairingCodeHandler)
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
        if (!keys.registered()) {
            return Optional.empty();
        }

        if (whatsapp == null) {
            this.whatsapp = Whatsapp.customBuilder()
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .build();
        }

        return Optional.of(whatsapp);
    }
}