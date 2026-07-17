package com.github.auties00.cobalt.wire.cloud.flow;

import java.time.Instant;
import java.util.Optional;

/**
 * A web preview link for a WhatsApp Cloud API Flow.
 *
 * <p>The preview is a short-lived URL that renders the Flow in a browser without sending it to a
 * recipient. This model carries the preview URL and the instant at which it expires, both as
 * returned by the Flow's {@code preview} field.
 */
public final class CloudFlowPreview {
    /**
     * The preview URL, or {@code null} when none was returned.
     */
    private final String previewUrl;

    /**
     * The expiry instant of the preview URL, or {@code null} when none was returned.
     */
    private final Instant expiresAt;

    /**
     * Constructs a new flow preview.
     *
     * @param previewUrl the preview URL, or {@code null} when none was returned
     * @param expiresAt  the expiry instant, or {@code null} when none was returned
     */
    public CloudFlowPreview(String previewUrl, Instant expiresAt) {
        this.previewUrl = previewUrl;
        this.expiresAt = expiresAt;
    }

    /**
     * Returns the preview URL.
     *
     * @return an {@link Optional} carrying the preview URL, or empty when none was returned
     */
    public Optional<String> previewUrl() {
        return Optional.ofNullable(previewUrl);
    }

    /**
     * Returns the expiry instant of the preview URL.
     *
     * @return an {@link Optional} carrying the expiry {@link Instant}, or empty when none was returned
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
