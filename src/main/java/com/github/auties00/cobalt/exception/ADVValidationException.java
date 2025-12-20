package com.github.auties00.cobalt.exception;

import com.github.auties00.cobalt.model.jid.Jid;

import java.util.Objects;

/**
 * A security exception that is thrown when ADV (Authenticated Device Verification) validation fails.
 * <p>
 * ADV is used to validate companion device identities to prevent MITM attacks.
 * This exception indicates that signature verification has failed during cryptographic operations.
 *
 * @see SecurityException
 */
public final class ADVValidationException extends SecurityException {
    private final Jid jid;
    private final Type type;

    /**
     * Constructs a new ADV validation exception.
     *
     * @param jid  the JID of the device that failed validation
     * @param type the type of validation failure
     */
    public ADVValidationException(Jid jid, Type type) {
        this(jid, type, null);
    }

    public ADVValidationException(Jid jid, Type type, Throwable cause) {
        Objects.requireNonNull(jid, "jid cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        var message = switch (type) {
            case MISSING_DEVICE_IDENTITY -> "Missing device-identity in prekey response for " + jid;
            case EMPTY_DEVICE_IDENTITY -> "Empty device-identity node for " + jid;
            case ACCOUNT_SIGNATURE_FAILED -> "ADV account signature verification failed for " + jid;
            case DEVICE_SIGNATURE_FAILED -> "ADV device signature verification failed for " + jid;
            case HMAC_VALIDATION_FAILED -> "ADV HMAC validation failed for " + jid;
            case CRYPTO_ERROR -> "ADV cryptographic operation failed for " + jid;
        };
        super(message, cause);
        this.jid = jid;
        this.type = type;
    }

    /**
     * Returns the JID of the device that failed validation.
     *
     * @return the device JID, or null if not available
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the type of validation failure.
     *
     * @return the failure type
     */
    public Type type() {
        return type;
    }

    /**
     * The type of ADV validation failure.
     */
    public enum Type {
        /**
         * The device-identity node is missing from the prekey response.
         */
        MISSING_DEVICE_IDENTITY,

        /**
         * The device-identity node contains empty or invalid data.
         */
        EMPTY_DEVICE_IDENTITY,

        /**
         * The account signature verification failed.
         */
        ACCOUNT_SIGNATURE_FAILED,

        /**
         * The device signature verification failed.
         */
        DEVICE_SIGNATURE_FAILED,

        /**
         * The HMAC validation of the device identity failed.
         */
        HMAC_VALIDATION_FAILED,

        /**
         * A cryptographic operation failed during ADV validation.
         */
        CRYPTO_ERROR
    }
}
