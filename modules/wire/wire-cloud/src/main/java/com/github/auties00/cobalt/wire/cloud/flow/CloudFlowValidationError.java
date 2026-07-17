package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.Objects;
import java.util.Optional;

/**
 * A validation error reported against a WhatsApp Cloud API Flow.
 *
 * <p>When a Flow's JSON fails validation the server returns one entry per problem under the Flow's
 * {@code validation_errors} field. This model carries the error code, a human-readable message, and
 * the optional source-location {@link Span} pointing into the Flow JSON document.
 */
public final class CloudFlowValidationError {
    /**
     * The machine-readable error code.
     */
    private final String code;

    /**
     * The error category, or {@code null} when none was returned.
     */
    private final String errorType;

    /**
     * The human-readable error message.
     */
    private final String message;

    /**
     * The source-location span pointing into the Flow JSON, or {@code null} when no location was
     * returned.
     */
    private final Span span;

    /**
     * Constructs a new validation error.
     *
     * @param code      the machine-readable error code
     * @param errorType the error category, or {@code null} when none was returned
     * @param message   the human-readable error message
     * @param span      the source-location span, or {@code null} when no location was returned
     * @throws NullPointerException if {@code code} or {@code message} is {@code null}
     */
    public CloudFlowValidationError(String code, String errorType, String message, Span span) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.errorType = errorType;
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.span = span;
    }

    /**
     * Returns the machine-readable error code.
     *
     * @return the error code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the error category.
     *
     * @return an {@link Optional} carrying the error category, or empty when none was returned
     */
    public Optional<String> errorType() {
        return Optional.ofNullable(errorType);
    }

    /**
     * Returns the human-readable error message.
     *
     * @return the message
     */
    public String message() {
        return message;
    }

    /**
     * Returns the source-location span pointing into the Flow JSON.
     *
     * @return an {@link Optional} carrying the {@link Span}, or empty when no location was returned
     */
    public Optional<Span> span() {
        return Optional.ofNullable(span);
    }

    /**
     * The source-location span of a {@link CloudFlowValidationError}, pointing into the Flow JSON
     * document.
     *
     * <p>The span groups the line and column ranges that delimit the offending region. The four
     * coordinates are reported together; the span is present only when the server returns a location.
     *
     * @param lineStart   the first line of the offending span
     * @param lineEnd     the last line of the offending span
     * @param columnStart the first column of the offending span
     * @param columnEnd   the last column of the offending span
     */
    public record Span(int lineStart, int lineEnd, int columnStart, int columnEnd) {
    }
}
