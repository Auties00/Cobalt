package com.github.auties00.cobalt.wire.cloud.waba;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The signature status Meta computed for a stored business encryption public key.
 *
 * <p>When a business uploads an RSA public key for encrypted payloads, Meta verifies the accompanying
 * signature: the key is {@link #VALID} when the signature checks out and {@link #MISMATCH} when it does
 * not. The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 */
@ProtobufEnum
public enum CloudBusinessEncryptionSignatureStatus {
    /**
     * A signature status that this client does not recognise. Resolved for any token outside the
     * modelled set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The signature checks out against the stored key.
     */
    VALID(1),

    /**
     * The signature does not match the stored key.
     */
    MISMATCH(2);

    /**
     * The protobuf-assigned numeric index for this signature status.
     */
    final int index;

    /**
     * Constructs a {@code CloudBusinessEncryptionSignatureStatus} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudBusinessEncryptionSignatureStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudBusinessEncryptionSignatureStatus} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "VALID"}, or {@code null}
     * @return the matching signature status, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudBusinessEncryptionSignatureStatus of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && value.name().equalsIgnoreCase(input)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the WhatsApp wire token for this signature status.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this signature status.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
