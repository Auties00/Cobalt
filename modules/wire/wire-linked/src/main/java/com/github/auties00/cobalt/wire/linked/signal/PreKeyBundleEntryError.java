package com.github.auties00.cobalt.wire.linked.signal;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * Carries a per-user error projection from the bulk Signal pre-key
 * bundle fetch operation.
 *
 * <p>Surfaces a relay-side rejection for a single addressee while the
 * rest of the batch may still carry successful
 * {@link PreKeyBundleEntry} bundles. Documented codes lie in the
 * {@code [500, 599]} range and indicate transient conditions
 * (rate-limit, registration mismatch, no pre-keys uploaded, etc.).
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class PreKeyBundleEntryError {
    /**
     * The per-user JID echoed by the relay.
     */
    private final Jid userJid;

    /**
     * The numeric error code (typically in {@code [500, 599]}).
     */
    private final int errorCode;

    /**
     * The human-readable error text.
     */
    private final String errorText;

    /**
     * Constructs a new per-user error projection.
     *
     * @param userJid   the per-user JID; never {@code null}
     * @param errorCode the numeric error code
     * @param errorText the human-readable text; never {@code null}
     * @throws NullPointerException if {@code userJid} or
     *                              {@code errorText} are {@code null}
     */
    public PreKeyBundleEntryError(Jid userJid, int errorCode, String errorText) {
        this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
        this.errorCode = errorCode;
        this.errorText = Objects.requireNonNull(errorText, "errorText cannot be null");
    }

    /**
     * Returns the per-user JID echoed by the relay.
     *
     * @return the JID; never {@code null}
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns the numeric error code.
     *
     * @return the error code
     */
    public int errorCode() {
        return errorCode;
    }

    /**
     * Returns the human-readable error text.
     *
     * @return the error text; never {@code null}
     */
    public String errorText() {
        return errorText;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PreKeyBundleEntryError) obj;
        return this.errorCode == that.errorCode
                && Objects.equals(this.userJid, that.userJid)
                && Objects.equals(this.errorText, that.errorText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userJid, errorCode, errorText);
    }

    @Override
    public String toString() {
        return "PreKeyBundleEntryError[userJid=" + userJid
                + ", errorCode=" + errorCode
                + ", errorText=" + errorText + ']';
    }
}
