package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Holds the success result of the status USync parser.
 *
 * Surfaced by USync queries that request the status protocol, such as the
 * background contact sync and the interactive "about" text fetch. Three states
 * are distinguishable by {@link #status()}: a present non-empty value carries
 * the live status text; a present empty string indicates the relay returned a
 * {@code code="401"} marker because the peer's privacy settings hide the
 * status; and an empty {@link Optional} indicates the peer has no status set.
 *
 * @implNote
 * This implementation preserves the JS tristate verbatim: the empty-string
 * "privacy hidden" sentinel and the {@code null} "no status" sentinel are
 * mirrored through the {@link Optional} contents rather than a dedicated
 * tristate enum so callers that only care about the text can pattern-match in
 * one line.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncStatus")
public final class StatusResult implements UsyncProtocolResponse {
    /**
     * Holds the status string, the empty string for the {@code code="401"}
     * privacy-hidden marker, or {@code null} when the peer has no status set.
     */
    private final String status;

    /**
     * Creates a new status result.
     *
     * @param status the status text, the empty string for the privacy-hidden
     *               marker, or {@code null} for "no status set"
     */
    public StatusResult(String status) {
        this.status = status;
    }

    /**
     * Returns the status, when present.
     *
     * The empty string is significant; callers that want to distinguish
     * "privacy hidden" from "no status set" must check {@link String#isEmpty()}
     * on the present value rather than collapsing both into a single null
     * check.
     *
     * @return the status, or empty when the peer has no status set
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }
}
