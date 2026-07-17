package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * The request to add a new phone number to a WhatsApp Business Account.
 *
 * <p>A business registers a phone number with its account by supplying the dialing country code, the
 * national phone number, and the verified display name to show in chats. The added number is created in
 * an unverified state and must complete the verification ceremony before it can send messages. This model
 * is the input to {@code CloudWhatsAppClient.addPhoneNumber}; all three fields are required.
 */
@ProtobufMessage
public final class CloudPhoneNumberAdd {
    /**
     * The dialing country code, for example {@code 1} or {@code 44}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String countryCode;

    /**
     * The national phone number, without the country code.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String phoneNumber;

    /**
     * The verified display name to show in chats.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String verifiedName;

    /**
     * Constructs a new add-phone-number request.
     *
     * @param countryCode  the dialing country code
     * @param phoneNumber  the national phone number
     * @param verifiedName the verified display name to show in chats
     * @throws NullPointerException if {@code countryCode}, {@code phoneNumber}, or {@code verifiedName} is
     *                              {@code null}
     */
    CloudPhoneNumberAdd(String countryCode, String phoneNumber, String verifiedName) {
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode must not be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        this.verifiedName = Objects.requireNonNull(verifiedName, "verifiedName must not be null");
    }

    /**
     * Returns the dialing country code.
     *
     * @return the country code
     */
    public String countryCode() {
        return countryCode;
    }

    /**
     * Returns the national phone number.
     *
     * @return the phone number
     */
    public String phoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns the verified display name to show in chats.
     *
     * @return the verified name
     */
    public String verifiedName() {
        return verifiedName;
    }
}
