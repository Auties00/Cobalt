package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Wrapper carrying a personally-identifying string value the server expects to receive as a
 * sensitive-string object.
 *
 * <p>Some Click-to-WhatsApp onboarding inputs (email addresses, verification codes, nonces) are not
 * sent as bare strings; the server expects each to be nested in a small object keyed by
 * {@code sensitive_string_value}. This model carries that single wrapped {@link #value() value} so the
 * caller supplies a plain string and the transport layer emits the wrapped shape.
 */
@ProtobufMessage(name = "SensitiveString")
public final class SensitiveString {
    /**
     * Wrapped string value. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String value;

    /**
     * Constructs a new {@code SensitiveString}. The {@code value} may be {@code null} to leave it
     * unset.
     *
     * @param value the wrapped string value, or {@code null}
     */
    SensitiveString(String value) {
        this.value = value;
    }

    /**
     * Returns the wrapped string value.
     *
     * @return an {@link Optional} carrying the value, or empty when unset
     */
    public Optional<String> value() {
        return Optional.ofNullable(value);
    }
}
