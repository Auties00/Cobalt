package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Optional;

/**
 * Holds the success result of the LID USync parser.
 *
 * Surfaced by USync queries that request the LID protocol, such as the
 * background contact sync, the delta sync, the existence probe, and the
 * contact-import verifier. Carries the LID alias resolved for the peer's
 * phone-number JID; absent when the peer has not been migrated to LID yet.
 *
 * @implNote
 * This implementation models the JS parser's return type as a nullable
 * {@link Jid}, where WA Web returns the bare {@code val} attribute when present
 * and {@code undefined} otherwise. The {@link Optional}-typed accessor
 * preserves the same tristate (success-with-lid, success-without-lid, error)
 * because the error variant is hoisted into {@link UsyncProtocolError}.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncLid")
public final class LidResult implements UsyncProtocolResponse {
    /**
     * Holds the resolved LID.
     *
     * Is {@code null} when the relay omitted the {@code val} attribute.
     */
    private final Jid lid;

    /**
     * Creates a new LID result.
     *
     * @param lid the resolved LID, or {@code null}
     */
    public LidResult(Jid lid) {
        this.lid = lid;
    }

    /**
     * Returns the resolved LID, when present.
     *
     * Empty when the peer has not been migrated to LID yet; the LID-based
     * contact sync uses that condition to keep the local mapping pinned to the
     * phone-number JID.
     *
     * @return the LID, or empty when absent
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }
}
