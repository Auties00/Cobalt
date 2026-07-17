package com.github.auties00.cobalt.wire.linked.error;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

/**
 * A numeric code that classifies a terminal failure reported by the WhatsApp
 * server inside an app-state synchronization patch.
 *
 * <p>During app-state synchronization the server ships patches that describe
 * incremental changes to collections such as chats, contacts, or starred
 * messages. If the server detects an unrecoverable condition while assembling
 * a patch it attaches a {@link DisconnectReason} that wraps one of these
 * codes. The client treats the affected collection as corrupted and triggers
 * a full re-synchronization.
 *
 * <p>Two variants are currently defined by the protocol. {@link MissingData}
 * with wire value {@code 100} indicates that the patch is missing mutation
 * data that the client would need to apply it. {@link DeserializationError}
 * with wire value {@code 101} indicates that the server itself could not
 * deserialize the patch contents and cannot send a well-formed payload.
 *
 * <p>Any value not recognized by the current protocol version is preserved in
 * an {@link Unknown} instance so that forward-compatible wire values do not
 * cause the client to crash. Unknown codes are still treated as fatal.
 *
 * <p>The wire representation is an unsigned 64-bit integer.
 *
 * @see DisconnectReason
 */
public sealed interface DisconnectCode {
    /**
     * A shared singleton for the {@code 100} exit code that signals a patch
     * with missing mutation data.
     */
    DisconnectCode MISSING_DATA = new MissingData();

    /**
     * A shared singleton for the {@code 101} exit code that signals a patch
     * the server could not deserialize.
     */
    DisconnectCode DESERIALIZATION_ERROR = new DeserializationError();

    /**
     * Returns the {@code DisconnectCode} that corresponds to the given wire
     * value.
     *
     * <p>Recognized values are {@code 100} (missing data) and {@code 101}
     * (deserialization error). Any other non-{@code null} value produces an
     * {@link Unknown} instance that preserves the original number so that
     * future protocol additions can still be observed and logged.
     *
     * @param value the wire value read from the protobuf stream, or
     *        {@code null} when the field is absent
     * @return the corresponding disconnect code, or {@code null} if
     *         {@code value} is {@code null}
     */
    @ProtobufDeserializer
    static DisconnectCode of(Long value) {
        if (value == null) {
            return null;
        }
        return switch (value.intValue()) {
            case 100 -> MISSING_DATA;
            case 101 -> DESERIALIZATION_ERROR;
            default -> new Unknown(value);
        };
    }

    /**
     * Returns the numeric value of this exit code as it appears on the wire.
     *
     * @return the unsigned 64-bit code
     */
    @ProtobufSerializer
    Long value();

    /**
     * A terminal exit code indicating that a synchronization patch is missing
     * mutation data that the client would need in order to apply the patch.
     *
     * <p>The wire value is {@code 100}.
     */
    record MissingData() implements DisconnectCode {
        /**
         * Returns the fixed wire value of this exit code.
         *
         * @return {@code 100L}
         */
        @Override
        public Long value() {
            return 100L;
        }
    }

    /**
     * A terminal exit code indicating that the server was unable to
     * deserialize the contents of a synchronization patch before sending it
     * to the client.
     *
     * <p>The wire value is {@code 101}.
     */
    record DeserializationError() implements DisconnectCode {
        /**
         * Returns the fixed wire value of this exit code.
         *
         * @return {@code 101L}
         */
        @Override
        public Long value() {
            return 101L;
        }
    }

    /**
     * A terminal exit code whose numeric value is not recognized by the
     * current protocol version.
     *
     * <p>Preserves the original wire value so that forward-compatible codes
     * introduced by the server can still be logged and reported. Consumers
     * should treat any {@code Unknown} code as a fatal synchronization error.
     *
     * @param value the raw numeric code as sent by the server
     */
    record Unknown(Long value) implements DisconnectCode {
    }
}
