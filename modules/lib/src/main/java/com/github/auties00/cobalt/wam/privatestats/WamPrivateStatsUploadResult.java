package com.github.auties00.cobalt.wam.privatestats;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Objects;

/**
 * Carries the categorised outcome of one private-stats buffer upload attempt.
 *
 * <p>Returned by {@link WamPrivateStatsUploader#upload(byte[])} so the caller can emit a
 * {@code PsBufferUploadWamEvent} mirroring the {@code WAWebUploadPrivateStatsBackend} flow. The
 * {@code httpResponseCode} is the HTTP status returned by the endpoint, or {@code -1} when the request never
 * reached the server.
 *
 * @param result           the categorised result code; see {@link Type}
 * @param httpResponseCode the HTTP status returned by the endpoint, or {@code -1} when the request never reached
 *                         the server (token issuance failure or transport exception)
 */
@WhatsAppWebModule(moduleName = "WAWebUploadPrivateStatsBackend")
@WhatsAppWebModule(moduleName = "WAWebWamEnumPsBufferUploadResult")
public record WamPrivateStatsUploadResult(Type result, int httpResponseCode) {
    /**
     * Validates the {@code result} component for {@code null}.
     *
     * @throws NullPointerException if {@code result} is {@code null}
     */
    public WamPrivateStatsUploadResult {
        Objects.requireNonNull(result, "result must not be null");
    }

    /**
     * Enumerates the categorised upload outcomes.
     *
     * <p>Mirrors the {@code PS_BUFFER_UPLOAD_RESULT} constants of the {@code WAWebWamEnumPsBufferUploadResult}
     * module, which {@code WAWebUploadPrivateStatsBackend} reads when populating
     * {@code PsBufferUploadWamEvent.psBufferUploadResult}.
     */
    public enum Type {
        /**
         * Indicates the server returned HTTP {@code 200}.
         */
        SUCCESS,
        /**
         * Indicates the server returned a transient server-side error, HTTP {@code 429} or {@code 500}.
         *
         * <p>WA Web treats these as retry-after-backoff outcomes.
         */
        ERROR_SERVER_OTHER,
        /**
         * Indicates the server returned HTTP {@code 400} because the multipart body could not be parsed.
         */
        ERROR_PARSING,
        /**
         * Indicates the server returned HTTP {@code 400} because the carried WAM buffer could not be decoded.
         *
         * <p>Cobalt does not currently distinguish between parse-failure and decode-failure 400 responses on the
         * wire; both map to {@link #ERROR_PARSING} in the default mapping.
         */
        ERROR_DECODING,
        /**
         * Indicates token issuance failed before the request was assembled.
         *
         * <p>Surfaced by {@link WamPrivateStatsUploader} when its {@link WamPrivateStatsTokenIssuer} throws.
         */
        ERROR_CREDENTIAL,
        /**
         * Indicates the server returned HTTP {@code 401} because the hard-coded {@code access_token} is no
         * longer accepted.
         */
        ERROR_ACCESS_TOKEN,
        /**
         * Indicates any other failure: a network error, an unexpected HTTP status, or a transport-layer
         * exception.
         */
        ERROR_OTHER
    }
}
