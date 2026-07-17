package com.github.auties00.cobalt.wire.cloud.phone;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * The end-to-end-encrypted-backup material restored while registering a phone number.
 *
 * <p>When a phone number that previously held an end-to-end-encrypted chat backup is re-registered, the
 * backup blob can be supplied so the account is restored rather than created empty. The blob is the
 * required {@link #data() data}; the {@link #password() password} is optional and present only when the
 * backup is protected by a user password rather than a 64-digit key. This model is the optional backup
 * argument to {@code CloudWhatsAppClient.registerPhoneNumber}.
 */
@ProtobufMessage
public final class CloudRegistrationBackup {
    /**
     * The encrypted backup blob.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String data;

    /**
     * The backup password, or {@code null} when the backup is key-protected rather than
     * password-protected.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String password;

    /**
     * Constructs a new registration-backup descriptor.
     *
     * @param data     the encrypted backup blob
     * @param password the backup password, or {@code null} when key-protected
     * @throws NullPointerException if {@code data} is {@code null}
     */
    CloudRegistrationBackup(String data, String password) {
        this.data = Objects.requireNonNull(data, "data must not be null");
        this.password = password;
    }

    /**
     * Returns the encrypted backup blob.
     *
     * @return the backup blob
     */
    public String data() {
        return data;
    }

    /**
     * Returns the backup password.
     *
     * @return an {@link Optional} carrying the password, or empty when the backup is key-protected
     */
    public Optional<String> password() {
        return Optional.ofNullable(password);
    }
}
