package com.github.auties00.cobalt.wire.cloud.waba;

import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API QR short-link that opens a chat with a prefilled message.
 *
 * <p>A QR short-link pairs a stable code with a prefilled message; scanning the QR or following the
 * deep link opens a chat with the business and pre-populates the composer. This model carries the
 * code, the prefilled message, and the resolved deep-link and QR-image URLs.
 */
public final class CloudQrCode {
    /**
     * The stable short-link code.
     */
    private final String code;

    /**
     * The message prefilled in the opened chat.
     */
    private final String prefilledMessage;

    /**
     * The resolved deep-link URL, or {@code null} when absent.
     */
    private final String deepLinkUrl;

    /**
     * The resolved QR-image URL, or {@code null} when absent.
     */
    private final String qrImageUrl;

    /**
     * Constructs a new QR short-link.
     *
     * @param code             the short-link code
     * @param prefilledMessage the prefilled message
     * @param deepLinkUrl      the resolved deep-link URL, or {@code null}
     * @param qrImageUrl       the resolved QR-image URL, or {@code null}
     * @throws NullPointerException if {@code code} or {@code prefilledMessage} is {@code null}
     */
    public CloudQrCode(String code, String prefilledMessage, String deepLinkUrl, String qrImageUrl) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.prefilledMessage = Objects.requireNonNull(prefilledMessage, "prefilledMessage must not be null");
        this.deepLinkUrl = deepLinkUrl;
        this.qrImageUrl = qrImageUrl;
    }

    /**
     * Returns the short-link code.
     *
     * @return the code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the prefilled message.
     *
     * @return the prefilled message
     */
    public String prefilledMessage() {
        return prefilledMessage;
    }

    /**
     * Returns the resolved deep-link URL.
     *
     * @return an {@link Optional} carrying the deep-link URL, or empty when absent
     */
    public Optional<String> deepLinkUrl() {
        return Optional.ofNullable(deepLinkUrl);
    }

    /**
     * Returns the resolved QR-image URL.
     *
     * @return an {@link Optional} carrying the QR-image URL, or empty when absent
     */
    public Optional<String> qrImageUrl() {
        return Optional.ofNullable(qrImageUrl);
    }
}
