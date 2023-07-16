package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public final class WebOptionsBuilder extends OptionsBuilder<WebOptionsBuilder> {
    private Whatsapp whatsapp;
    private WebOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    static WebOptionsBuilder of(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, ClientType.WEB);
        var store = Store.of(uuid, ClientType.WEB, serializer);
        var keys = Keys.of(uuid, ClientType.WEB, serializer);
        return new WebOptionsBuilder(store, keys);
    }

    static Optional<WebOptionsBuilder> ofNullable(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, ClientType.WEB);
        var store = Store.ofNullable(uuid, ClientType.WEB, serializer);
        var keys = Keys.ofNullable(uuid, ClientType.WEB, serializer);
        if(store.isEmpty() || keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new WebOptionsBuilder(store.get(), keys.get()));
    }

    static WebOptionsBuilder of(long phoneNumber, ControllerSerializer serializer){
        var uuid = UUID.randomUUID();
        var store = Store.of(uuid, phoneNumber, ClientType.WEB, serializer);
        var keys = Keys.of(uuid, phoneNumber, ClientType.WEB, serializer);
        return new WebOptionsBuilder(store, keys);
    }

    static Optional<WebOptionsBuilder> ofNullable(Long phoneNumber, ControllerSerializer serializer){;
        var store = Store.ofNullable(phoneNumber, ClientType.WEB, serializer);
        var keys = Keys.ofNullable(phoneNumber, ClientType.WEB, serializer);
        if(store.isEmpty() || keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new WebOptionsBuilder(store.get(), keys.get()));
    }

    static WebOptionsBuilder of(String alias, ControllerSerializer serializer){
        var uuid = UUID.randomUUID();
        var store = Store.of(uuid, alias, ClientType.WEB, serializer);
        var keys = Keys.of(uuid, alias, ClientType.WEB, serializer);
        return new WebOptionsBuilder(store, keys);
    }

    static Optional<WebOptionsBuilder> ofNullable(String alias, ControllerSerializer serializer){
        var store = Store.ofNullable(alias, ClientType.WEB, serializer);
        var keys = Keys.ofNullable(alias, ClientType.WEB, serializer);
        if(store.isEmpty() || keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new WebOptionsBuilder(store.get(), keys.get()));
    }

    /**
     * Sets how much chat history Whatsapp should send when the QR is first scanned.
     * By default, one year
     *
     * @return the same instance for chaining
     */
    public WebOptionsBuilder historyLength(@NonNull WebHistoryLength historyLength) {
        store.historyLength(historyLength);
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
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .socketExecutor(socketExecutor)
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
    public Whatsapp unregistered(long phoneNumber, @NonNull PairingCodeHandler pairingCodeHandler) {
        if (whatsapp == null) {
            store.phoneNumber(PhoneNumber.of(phoneNumber));
            this.whatsapp = Whatsapp.customBuilder()
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .socketExecutor(socketExecutor)
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
        if(!keys.registered()){
            return Optional.empty();
        }

        if (whatsapp == null) {
            this.whatsapp = Whatsapp.customBuilder()
                    .store(store)
                    .keys(keys)
                    .errorHandler(errorHandler)
                    .socketExecutor(socketExecutor)
                    .build();
        }

        return Optional.of(whatsapp);
    }
}