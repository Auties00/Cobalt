package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Records a single CTWA (Click-to-WhatsApp) external entry point captured when a user opens a chat via a CTWA ad link.
 * <p>
 * Stored by {@link CtwaAttributionStanza} keyed by chat JID; each entry lives at most {@link #MAX_AGE} (one week) before
 * {@link CtwaAttributionStanza#build(com.github.auties00.cobalt.wire.core.jid.Jid)} begins ignoring it and the save method
 * prunes it.
 *
 * @param deepLinkType the deep-link type token that led the user to this chat (e.g. {@code "WA_HOOK"})
 * @param authSuccess  whether the ad-flow authentication succeeded
 * @param partnerName  the partner or advertiser name, or {@code null} when not provided
 * @param addedTime    the {@link Instant} at which this entry was recorded
 */
@WhatsAppWebModule(moduleName = "WAWebExternalEntryPointPrefs")
public record ExternalEntryPoint(
        String deepLinkType,
        boolean authSuccess,
        String partnerName,
        Instant addedTime
) {
    /**
     * Holds the maximum age of an external entry point before it is considered expired and dropped from the entry-point
     * cache.
     */
    @WhatsAppWebExport(moduleName = "WATimeUtils", exports = "WEEK_MILLISECONDS",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final Duration MAX_AGE = Duration.ofDays(7);

    /**
     * Returns whether this entry point is older than {@link #MAX_AGE}.
     * <p>
     * Used both by {@link CtwaAttributionStanza#getEntryPoint(com.github.auties00.cobalt.wire.core.jid.Jid)} to decline
     * returning expired entries and by the in-memory pruning step on save.
     *
     * @implNote This implementation uses a strict greater-than comparison so the boundary instant
     * ({@code addedTime + MAX_AGE} exactly) is still considered valid.
     *
     * @return {@code true} when this entry has expired
     */
    @WhatsAppWebExport(moduleName = "WAWebExternalEntryPointPrefs", exports = "getExternalEntryPoint",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean isExpired() {
        return Duration.between(addedTime, Instant.now()).compareTo(MAX_AGE) > 0;
    }

    /**
     * Returns the partner name as an {@link Optional}.
     * <p>
     * Convenience accessor for callers that want to flat-map on the presence of a partner name rather than null-check
     * the record component.
     *
     * @return the partner name, or empty when {@code null}
     */
    public Optional<String> optionalPartnerName() {
        return Optional.ofNullable(partnerName);
    }
}
