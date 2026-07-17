package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * Holds the success result of the business USync parser.
 *
 * Surfaced by USync queries that request the business protocol, such as the
 * contact-import verifier, the background contact sync, the per-contact
 * verified-name fetch, and the username probe. The raw {@code <verified_name>}
 * {@link Stanza} is exposed so callers can pipe it through their own
 * verified-name protobuf decoder rather than decoding it here.
 *
 * @implNote
 * This implementation keeps the verified-name decoding out of the result type
 * because the verified-name decoder lives in a sibling module shared with
 * notification handlers.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncBusiness")
public final class BusinessResult implements UsyncProtocolResponse {
    /**
     * Holds the raw {@code <verified_name>} child {@link Stanza}.
     *
     * Is {@code null} when the relay did not return one for this peer.
     */
    private final Stanza verifiedName;

    /**
     * Creates a new business result.
     *
     * @param verifiedName the raw {@code <verified_name>} {@link Stanza}, or
     *                     {@code null}
     */
    public BusinessResult(Stanza verifiedName) {
        this.verifiedName = verifiedName;
    }

    /**
     * Returns the raw {@code <verified_name>} {@link Stanza}, when present.
     *
     * Decode through the verified-name protobuf reader to extract the business
     * name, issuer, and serial. Absent when the peer is not a verified
     * business.
     *
     * @return the verified-name {@link Stanza}, or empty when absent
     */
    public Optional<Stanza> verifiedName() {
        return Optional.ofNullable(verifiedName);
    }
}
