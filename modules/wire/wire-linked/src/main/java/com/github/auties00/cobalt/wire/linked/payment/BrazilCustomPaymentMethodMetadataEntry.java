package com.github.auties00.cobalt.wire.linked.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Key/value entry carried inside the metadata bag of a
 * {@link BrazilCustomPaymentMethodCreate} request. Exposes the bag as
 * an ordered list of typed entries so the public surface carries no
 * {@code Map}.
 */
@ProtobufMessage
public final class BrazilCustomPaymentMethodMetadataEntry {
    /**
     * Metadata key.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String key;

    /**
     * Metadata value.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String value;

    /**
     * Constructs a new {@code BrazilCustomPaymentMethodMetadataEntry}.
     *
     * @param key   the metadata key; required
     * @param value the metadata value; required
     * @throws NullPointerException if any argument is {@code null}
     */
    BrazilCustomPaymentMethodMetadataEntry(String key, String value) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * Returns the metadata key.
     *
     * @return the key, never {@code null}
     */
    public String key() {
        return key;
    }

    /**
     * Returns the metadata value.
     *
     * @return the value, never {@code null}
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BrazilCustomPaymentMethodMetadataEntry) obj;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "BrazilCustomPaymentMethodMetadataEntry[" +
                "key=" + key + ", " +
                "value=" + value + ']';
    }
}
