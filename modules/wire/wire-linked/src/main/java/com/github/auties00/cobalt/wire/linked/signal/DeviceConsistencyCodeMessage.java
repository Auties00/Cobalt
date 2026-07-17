package com.github.auties00.cobalt.wire.linked.signal;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a device consistency code used to protect against active
 * man-in-the-middle attacks during the Signal protocol key exchange.
 *
 * <p>When two WhatsApp users verify their safety numbers, each device derives
 * a short consistency code from the identity keys of every device registered
 * to the pair. If an attacker has inserted a rogue device into either side,
 * the codes will not match, making the tampering detectable. The code is
 * signed by the sender's identity key and exchanged inside this message so
 * the receiver can confirm that the code really comes from the claimed
 * identity.
 *
 * <p>The {@code generation} field increments every time the set of devices
 * changes, allowing both sides to agree on which snapshot of the device list
 * the signature covers.
 */
@ProtobufMessage(name = "DeviceConsistencyCodeMessage")
public final class DeviceConsistencyCodeMessage {
    /**
     * Monotonically increasing counter that identifies the version of the
     * device list this code was computed over. Both peers must reference
     * the same generation for the codes to match.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer generation;

    /**
     * The signature produced by the sender's identity key over the encoded
     * consistency code. The receiver verifies this signature before accepting
     * the code as authentic.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] signature;

    /**
     * Constructs a new device consistency code message.
     *
     * @param generation the generation counter, or {@code null} if not set
     * @param signature  the signature bytes, or {@code null} if not set
     */
    DeviceConsistencyCodeMessage(Integer generation, byte[] signature) {
        this.generation = generation;
        this.signature = signature;
    }

    /**
     * Returns the generation counter of the device list this code was
     * computed over.
     *
     * @return an {@link OptionalInt} containing the generation, or
     *         {@link OptionalInt#empty()} if not set
     */
    public OptionalInt generation() {
        return generation == null ? OptionalInt.empty() : OptionalInt.of(generation);
    }

    /**
     * Returns the signature bytes authenticating the consistency code.
     *
     * @return an {@link Optional} containing the signature bytes, or
     *         {@link Optional#empty()} if not set
     */
    public Optional<byte[]> signature() {
        return Optional.ofNullable(signature);
    }

    /**
     * Replaces the generation counter.
     *
     * @param generation the new generation value, or {@code null} to clear
     */
    public void setGeneration(Integer generation) {
        this.generation = generation;
    }

    /**
     * Replaces the signature bytes.
     *
     * @param signature the new signature, or {@code null} to clear
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}
