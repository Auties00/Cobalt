package com.github.auties00.cobalt.wire.stanza.smax.psa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Enumerates the two documented values of the {@code status} attribute on a
 * {@code <blocking>} child of a PSA chat-block IQ result.
 *
 * <p>The two constants project the boolean PSA-mute flag carried over the
 * wire: {@link #BLOCKED} stands for the muted state and {@link #UNBLOCKED}
 * for the delivering state. The wire only ever carries the literals
 * {@code "blocked"} and {@code "unblocked"}; any other token fails to
 * resolve through {@link #ofWire(String)}.
 */
@WhatsAppWebModule(moduleName = "WASmaxInPsaEnums")
public enum SmaxPsaChatBlockGetBlockingStatus {
    /**
     * Denotes that the PSA broadcast channel is currently muted server-side
     * for this user.
     */
    BLOCKED("blocked"),
    /**
     * Denotes that the PSA broadcast channel is currently delivering to this
     * user.
     */
    UNBLOCKED("unblocked");

    /**
     * Holds the wire-level literal carried by the {@code status} attribute.
     */
    private final String wireValue;

    /**
     * Constructs an enum constant bound to its wire-level literal.
     *
     * @param wireValue the wire-level literal; never {@code null}
     */
    SmaxPsaChatBlockGetBlockingStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-level literal carried by the {@code status} attribute.
     *
     * @return the wire-level literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves a constant from its wire-level literal.
     *
     * <p>Returns {@link Optional#empty()} when the supplied literal is
     * {@code null} or is not one of the two documented values, which lets a
     * caller treat the unknown token as a parse miss rather than a thrown
     * failure.
     *
     * @param wireValue the wire-level literal; may be {@code null}
     * @return an {@link Optional} carrying the resolved enum constant, or
     *         empty when the literal is not one of the documented values
     */
    public static Optional<SmaxPsaChatBlockGetBlockingStatus> ofWire(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
