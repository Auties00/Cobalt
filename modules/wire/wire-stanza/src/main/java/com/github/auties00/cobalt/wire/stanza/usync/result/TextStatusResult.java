package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.time.Duration;
import java.util.Optional;

/**
 * Holds the success result of the text-status USync parser.
 *
 * Surfaced by USync queries that request the text-status protocol, such as the
 * background contact sync. Carries the peer's modern text-status payload
 * (text, leading emoji, ephemeral lifetime, last-update timestamp); every
 * field is nullable on the wire and the relay sets only those the peer
 * published.
 *
 * @implNote
 * This implementation reads the emoji glyph from the {@code content} attribute
 * of the {@code <emoji>} child because the modern text-status uses an
 * attribute, not inline content. The {@link #lastUpdateTime()} value is kept
 * as a {@link String} because the relay echoes the value verbatim and WA Web
 * does not parse it into a typed instant.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncTextStatus")
public final class TextStatusResult implements UsyncProtocolResponse {
    /**
     * Holds the status text from the {@code text} attribute.
     *
     * Is {@code null} when absent.
     */
    private final String text;

    /**
     * Holds the leading emoji glyph from the {@code <emoji>} child's
     * {@code content} attribute.
     *
     * Is {@code null} when absent.
     */
    private final String emoji;

    /**
     * Holds the status's ephemeral lifetime decoded from the
     * {@code ephemeral_duration_sec} attribute.
     *
     * Is {@code null} when absent.
     */
    private final Duration ephemeralDuration;

    /**
     * Holds the {@code last_update_time} attribute echoed verbatim by the
     * relay.
     *
     * Is {@code null} when absent.
     */
    private final String lastUpdateTime;

    /**
     * Creates a new text-status result.
     *
     * @param text              the status text, or {@code null}
     * @param emoji             the leading emoji glyph, or {@code null}
     * @param ephemeralDuration the ephemeral lifetime, or {@code null}
     * @param lastUpdateTime    the {@code last_update_time} attribute, or
     *                          {@code null}
     */
    public TextStatusResult(String text, String emoji, Duration ephemeralDuration, String lastUpdateTime) {
        this.text = text;
        this.emoji = emoji;
        this.ephemeralDuration = ephemeralDuration;
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * Returns the status text, when present.
     *
     * Body of the status chip shown beside the peer's name in the chat header.
     *
     * @return the text, or empty when absent
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the leading emoji glyph, when present.
     *
     * Decorative emoji rendered to the left of the status text; sourced from
     * the {@code content} attribute of the {@code <emoji>} child, not from the
     * element's inline content.
     *
     * @return the emoji, or empty when absent
     */
    public Optional<String> emoji() {
        return Optional.ofNullable(emoji);
    }

    /**
     * Returns the ephemeral lifetime, when present.
     *
     * Wall-clock duration the status remains visible for; absent for permanent
     * statuses.
     *
     * @return the lifetime, or empty when absent
     */
    public Optional<Duration> ephemeralDuration() {
        return Optional.ofNullable(ephemeralDuration);
    }

    /**
     * Returns the {@code last_update_time} attribute, when present.
     *
     * Kept as a {@link String} because the relay echoes the value verbatim and
     * WA Web does not decode it into a typed instant; callers that need a
     * timestamp parse it themselves.
     *
     * @return the timestamp string, or empty when absent
     */
    public Optional<String> lastUpdateTime() {
        return Optional.ofNullable(lastUpdateTime);
    }
}
