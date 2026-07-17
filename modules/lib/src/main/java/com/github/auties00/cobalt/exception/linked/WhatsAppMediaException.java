package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.OptionalInt;

/**
 * Sealed root for failures during media operations: uploads, downloads,
 * media-server connection bring-up, and the local processing around them.
 *
 * WhatsApp media (images, videos, audio messages, documents, stickers)
 * travels over a separate set of CDN endpoints from the main messaging
 * WebSocket. The flow asks the server for a fresh media connection,
 * encrypts and uploads the bytes (or fetches and decrypts them during a
 * download), then runs any local processing (thumbnailing, format
 * conversion, integrity checks). Each step surfaces through one of the
 * nested subtypes. When the failure originated from an HTTP response, the
 * originating status code is preserved on {@link #httpStatusCode()} and
 * matches one of the {@code HTTP_*} constants on this class. The permits
 * list is closed, so a {@code switch} over a {@code WhatsAppMediaException}
 * can be exhaustive.
 *
 * @apiNote
 * Inspect {@link #httpStatusCode()} to react differently to a
 * {@link #HTTP_UNAUTHORIZED} (refresh auth), a {@link #HTTP_TOO_LARGE}
 * (give up), or a {@link #HTTP_THROTTLE} (back off). Every subtype returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} from {@link #toErrorResult()},
 * so a failed transfer can be retried or abandoned without tearing the
 * messaging session down.
 *
 * @implNote
 * This implementation always returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD}: media operations are isolated
 * from the main messaging channel, so a failed transfer is retried or
 * abandoned without tearing the session down.
 *
 * @see Connection
 * @see Upload
 * @see Download
 * @see Processing
 */
@WhatsAppWebModule(moduleName = "WAWebMmsClientErrors")
@WhatsAppWebModule(moduleName = "WAWebHttpErrors")
public sealed class WhatsAppMediaException extends WhatsAppLinkedException
        permits WhatsAppMediaException.Download,
                WhatsAppMediaException.Processing,
                WhatsAppMediaException.Upload,
                WhatsAppMediaException.Connection {

    /**
     * The {@code 401} status raised by the media CDN when the upload or
     * download authentication token has expired.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientErrors", exports = "MMSUnauthorizedError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int HTTP_UNAUTHORIZED = 401;

    /**
     * The {@code 403} status raised by the media CDN when access to the
     * resource is denied.
     *
     * When the response body indicates an expired URL signature, the
     * server-side classification reclassifies the failure as a not-found
     * error instead.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientErrors", exports = "MMSForbiddenError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int HTTP_FORBIDDEN = 403;

    /**
     * The {@code 404} status raised by the media CDN when the file is not
     * available.
     *
     * Raised either because the file has been deleted or because the
     * download URL has expired. HTTP {@code 410 Gone} is treated
     * identically.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientErrors", exports = "MediaNotFoundError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int HTTP_NOT_FOUND = 404;

    /**
     * The {@code 413} status raised by the media CDN when the uploaded
     * payload exceeds the server-side size limit.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientErrors", exports = "MediaTooLargeError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int HTTP_TOO_LARGE = 413;

    /**
     * The {@code 415} status raised by the media CDN when the media format
     * is invalid or the ciphertext hash does not match the value the client
     * computed.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientErrors", exports = "MediaInvalidError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int HTTP_INVALID_MEDIA = 415;

    /**
     * The {@code 507} status raised by the media CDN when the server is
     * throttling traffic from this client.
     *
     * @apiNote
     * Embedders should back off rather than re-attempt the transfer; this
     * code is not retryable.
     */
    @WhatsAppWebExport(moduleName = "WAWebMmsClientErrors", exports = "MMSThrottleError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int HTTP_THROTTLE = 507;

    /**
     * The HTTP status code returned by the CDN, or {@code -1} when the
     * failure did not originate from an HTTP response.
     */
    private final int httpStatusCode;

    /**
     * Constructs a new media exception with the specified detail message.
     *
     * @param message the detail message describing the media error
     */
    public WhatsAppMediaException(String message) {
        super(message);
        this.httpStatusCode = -1;
    }

    /**
     * Constructs a new media exception with the specified HTTP status code and detail message.
     *
     * @param httpStatusCode the HTTP status code returned by the media CDN
     * @param message        the detail message describing the media error
     */
    @WhatsAppWebExport(moduleName = "WAWebHttpErrors", exports = "HttpStatusCodeError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public WhatsAppMediaException(int httpStatusCode, String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Constructs a new media exception with the specified detail message and cause.
     *
     * @param message the detail message describing the media error
     * @param cause   the underlying cause of this exception
     */
    public WhatsAppMediaException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = -1;
    }

    /**
     * Constructs a new media exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public WhatsAppMediaException(Throwable cause) {
        super(cause);
        this.httpStatusCode = -1;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: media operations are
     * isolated from the main messaging channel, so a failed transfer is
     * retried or abandoned without tearing the session down.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Returns the HTTP status code returned by the media CDN, when the
     * failure originated from an HTTP response.
     *
     * @apiNote
     * Match the returned value against the {@code HTTP_*} constants on
     * this class to decide how to react (refresh auth on
     * {@link #HTTP_UNAUTHORIZED}, re-request a URL on
     * {@link #HTTP_NOT_FOUND}, give up on {@link #HTTP_TOO_LARGE}, back
     * off on {@link #HTTP_THROTTLE}).
     *
     * @return the HTTP status code wrapped in an {@link OptionalInt}, or
     *         empty when the failure was not produced by an HTTP response
     */
    public OptionalInt httpStatusCode() {
        return httpStatusCode == -1 ? OptionalInt.empty() : OptionalInt.of(httpStatusCode);
    }

    /**
     * Thrown when the media-server connection cannot be established or is
     * no longer usable.
     *
     * WhatsApp serves media through endpoints negotiated dynamically and
     * with a limited lifetime. Triggered when the negotiation failed, the
     * connection expired during a transfer, the server returned no usable
     * hosts, or the TLS handshake to the chosen host failed.
     */
    @WhatsAppWebModule(moduleName = "WAWebMediaHostsErrors")
    public static final class Connection extends WhatsAppMediaException {
        /**
         * Constructs a new media connection exception with a default message.
         */
        public Connection() {
            super("Media connection failed");
        }

        /**
         * Constructs a new media connection exception with the specified message.
         *
         * @param message the detail message describing the connection failure
         */
        public Connection(String message) {
            super(message);
        }

        /**
         * Constructs a new media connection exception with the specified message and cause.
         *
         * @param message the detail message describing the connection failure
         * @param cause   the underlying cause of the connection failure
         */
        public Connection(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new media connection exception wrapping the specified cause.
         *
         * @param cause the underlying cause of the connection failure
         */
        public Connection(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Thrown when fetching a media file from the WhatsApp CDN fails.
     *
     * Aggregates a deleted file ({@link #HTTP_NOT_FOUND}), an expired auth
     * token ({@link #HTTP_UNAUTHORIZED}), rate limiting
     * ({@link #HTTP_THROTTLE}), a transport failure, or a deadline
     * exceeded, plus integrity-validation failures Cobalt detects during
     * decryption.
     */
    @WhatsAppWebModule(moduleName = "WAWebMmsClientErrors")
    @WhatsAppWebModule(moduleName = "WAWebMmsClientMmsDownload")
    public static final class Download extends WhatsAppMediaException {
        /**
         * Constructs a new media download exception with the specified message.
         *
         * @param message the detail message describing the download failure
         */
        public Download(String message) {
            super(message);
        }

        /**
         * Constructs a new media download exception with the specified HTTP status code and message.
         *
         * @param httpStatusCode the HTTP status code returned by the CDN
         * @param message        the detail message describing the download failure
         */
        public Download(int httpStatusCode, String message) {
            super(httpStatusCode, message);
        }

        /**
         * Constructs a new media download exception with the specified message and cause.
         *
         * @param message the detail message describing the download failure
         * @param cause   the underlying cause of the download failure
         */
        @WhatsAppWebExport(moduleName = "WAWebHttpErrors", exports = "HttpNetworkError",
                           adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebHttpErrors", exports = "HttpTimedOutError",
                           adaptation = WhatsAppAdaptation.ADAPTED)
        public Download(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new media download exception wrapping the specified cause.
         *
         * @param cause the underlying cause of the download failure
         */
        public Download(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Thrown when uploading a media file to the WhatsApp CDN fails.
     *
     * Triggered when the media connection is no longer valid, the network
     * call could not complete, or the server rejected the upload. Common
     * server-side rejections are {@link #HTTP_TOO_LARGE} for oversize
     * content, {@link #HTTP_INVALID_MEDIA} for a hash or format mismatch,
     * {@link #HTTP_UNAUTHORIZED} for an expired auth token, and
     * {@link #HTTP_THROTTLE} for rate limiting.
     */
    @WhatsAppWebModule(moduleName = "WAWebMmsClientErrors")
    public static final class Upload extends WhatsAppMediaException {
        /**
         * Constructs a new media upload exception with the specified message.
         *
         * @param message the detail message describing the upload failure
         */
        public Upload(String message) {
            super(message);
        }

        /**
         * Constructs a new media upload exception with the specified HTTP status code and message.
         *
         * @param httpStatusCode the HTTP status code returned by the CDN
         * @param message        the detail message describing the upload failure
         */
        public Upload(int httpStatusCode, String message) {
            super(httpStatusCode, message);
        }

        /**
         * Constructs a new media upload exception with the specified message and cause.
         *
         * @param message the detail message describing the upload failure
         * @param cause   the underlying cause of the upload failure
         */
        public Upload(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new media upload exception wrapping the specified cause.
         *
         * @param cause the underlying cause of the upload failure
         */
        public Upload(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Thrown when local processing of a media file fails.
     *
     * Processing covers everything around the transfer itself: encryption
     * and decryption, thumbnail and waveform generation, format conversion,
     * metadata extraction, and integrity checks on responses that parsed
     * but contain unusable content.
     */
    @WhatsAppWebModule(moduleName = "WAWebHttpErrors")
    public static final class Processing extends WhatsAppMediaException {
        /**
         * Constructs a new media processing exception with the specified message.
         *
         * @param message the detail message describing the processing failure
         */
        @WhatsAppWebExport(moduleName = "WAWebHttpErrors", exports = "HttpInvalidResponseError",
                           adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WAWebHttpErrors", exports = "MmsDownloadFilehashMismatchError",
                           adaptation = WhatsAppAdaptation.ADAPTED)
        public Processing(String message) {
            super(message);
        }

        /**
         * Constructs a new media processing exception with the specified message and cause.
         *
         * @param message the detail message describing the processing failure
         * @param cause   the underlying cause of the processing failure
         */
        public Processing(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new media processing exception wrapping the specified cause.
         *
         * @param cause the underlying cause of the processing failure
         */
        public Processing(Throwable cause) {
            super(cause);
        }
    }
}
