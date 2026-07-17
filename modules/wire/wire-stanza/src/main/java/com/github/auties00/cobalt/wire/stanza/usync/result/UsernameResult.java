package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Holds the success result of the username USync parser.
 *
 * Surfaced by USync queries that request the username protocol, such as the
 * background contact sync, the delta sync, and the username probe used by the
 * "find on WhatsApp" flow. Carries the peer's claimed username; empty when the
 * peer has not claimed one.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncUsername")
public final class UsernameResult implements UsyncProtocolResponse {
    /**
     * Holds the peer's username from the inline content of the
     * {@code <username>} element.
     *
     * Is {@code null} when the element had no content.
     */
    private final String username;

    /**
     * Creates a new username result.
     *
     * @param username the peer's username, or {@code null}
     */
    public UsernameResult(String username) {
        this.username = username;
    }

    /**
     * Returns the peer's username, when present.
     *
     * Empty when the peer has not claimed a username yet; the username probe
     * uses that condition to fall back to phone-number addressing.
     *
     * @return the username, or empty when absent
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }
}
