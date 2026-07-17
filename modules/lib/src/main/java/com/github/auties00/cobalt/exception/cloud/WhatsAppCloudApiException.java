package com.github.auties00.cobalt.exception.cloud;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Thrown when the graph endpoint rejected a Cloud API request with a structured error envelope.
 *
 * <p>Carries the HTTP status and the {@code error} object Meta returns ({@code code},
 * {@code error_subcode}, {@code message}, and the {@code fbtrace_id} support correlation token),
 * so callers can classify the rejection (a rate limit, an invalid parameter, a re-engagement
 * window violation) and log the trace id when escalating to Meta.
 */
public final class WhatsAppCloudApiException extends WhatsAppCloudException {
    /**
     * The HTTP status code returned by the graph endpoint, or {@code null} when the failure
     * occurred before a response was received.
     */
    private final Integer httpStatus;

    /**
     * The Meta {@code error.code} value, or {@code null} when the response carried none.
     */
    private final Integer code;

    /**
     * The Meta {@code error.error_subcode} value, or {@code null} when the response carried
     * none.
     */
    private final Integer subcode;

    /**
     * The Meta {@code error.fbtrace_id} support correlation token, or {@code null} when absent.
     */
    private final String fbtraceId;

    /**
     * Constructs a new Cloud API exception with no transport-level detail.
     *
     * <p>Used when the failure occurred before a structured graph response was available, for
     * example a transport I/O error or an interrupted send, so the HTTP status, error code,
     * subcode, and trace id are all absent.
     *
     * @param message the detail message describing the failure
     */
    public WhatsAppCloudApiException(String message) {
        super(message);
        this.httpStatus = null;
        this.code = null;
        this.subcode = null;
        this.fbtraceId = null;
    }

    /**
     * Constructs a new Cloud API exception.
     *
     * @param httpStatus the HTTP status returned by the graph endpoint
     * @param code       the Meta {@code error.code}, or {@code 0} when absent
     * @param subcode    the Meta {@code error.error_subcode}, or {@code 0} when absent
     * @param message    the Meta {@code error.message}, or a synthesised description
     * @param fbtraceId  the Meta {@code error.fbtrace_id}, or {@code null} when absent
     */
    public WhatsAppCloudApiException(int httpStatus, int code, int subcode, String message, String fbtraceId) {
        super("Cloud API request failed: status=" + httpStatus + ", code=" + code
                + ", subcode=" + subcode + ", fbtrace_id=" + fbtraceId + ", message=" + message);
        this.httpStatus = httpStatus;
        this.code = code == 0 ? null : code;
        this.subcode = subcode == 0 ? null : subcode;
        this.fbtraceId = fbtraceId;
    }

    /**
     * Returns the HTTP status returned by the graph endpoint.
     *
     * @return an {@link OptionalInt} carrying the HTTP status code, or empty when the failure
     *         occurred before a response was received
     */
    public OptionalInt httpStatus() {
        return httpStatus == null ? OptionalInt.empty() : OptionalInt.of(httpStatus);
    }

    /**
     * Returns the Meta {@code error.code} value.
     *
     * @return an {@link OptionalInt} carrying the error code, or empty when the response
     *         carried none
     */
    public OptionalInt code() {
        return code == null ? OptionalInt.empty() : OptionalInt.of(code);
    }

    /**
     * Returns the Meta {@code error.error_subcode} value.
     *
     * @return an {@link OptionalInt} carrying the error subcode, or empty when the response
     *         carried none
     */
    public OptionalInt subcode() {
        return subcode == null ? OptionalInt.empty() : OptionalInt.of(subcode);
    }

    /**
     * Returns the Meta {@code error.fbtrace_id} support correlation token.
     *
     * @return an {@link Optional} carrying the trace id, or empty when the response carried
     *         none
     */
    public Optional<String> fbtraceId() {
        return Optional.ofNullable(fbtraceId);
    }
}
