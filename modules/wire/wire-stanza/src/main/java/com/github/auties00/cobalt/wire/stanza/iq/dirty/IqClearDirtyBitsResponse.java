package com.github.auties00.cobalt.wire.stanza.iq.dirty;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants the relay produces in response to an
 * {@link IqClearDirtyBitsRequest}.
 *
 * <p>The hierarchy permits exactly {@link Success}, {@link ClientError}, and
 * {@link ServerError}, splitting an accepted clear from a permanent rejection
 * ({@link ClientError}) and from a transient relay failure ({@link ServerError}). This split
 * lets the dirty-bit driver decide whether the resource should be marked as not-yet-cleared
 * (transient failure) or whether the entire dirty-bit pass should be aborted (permanent
 * failure).
 *
 * @implNote
 * This implementation diverges from WA Web's {@code cleanDirtyReplyParser}, which only asserts
 * {@code type="result"} and logs failures to the warn channel without surfacing them; the
 * three-variant split is a Cobalt redesign that exposes the failure mode to the dirty-bit
 * driver instead.
 */
@WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
public sealed interface IqClearDirtyBitsResponse extends IqStanza.Response
        permits IqClearDirtyBitsResponse.Success, IqClearDirtyBitsResponse.ClientError, IqClearDirtyBitsResponse.ServerError {

    /**
     * Tries each {@link IqClearDirtyBitsResponse} variant in priority order and returns the
     * first that parses cleanly.
     *
     * <p>Variants are attempted in the order {@link Success}, {@link ClientError},
     * {@link ServerError}. At most one variant ever populates because
     * {@link SmaxIqResultResponseMixin} and {@link SmaxBaseServerErrorMixin} match disjoint
     * stanza shapes.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()}
     *         when no documented variant matched the stanza shape
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebClearDirtyBitsJob",
            exports = "clearDirtyBits", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqClearDirtyBitsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Represents the reply variant signalling that the relay accepted the dirty-bit clear.
     *
     * <p>Carries no payload beyond the echoed request id.
     */
    @WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
    final class Success implements IqClearDirtyBitsResponse {
        /**
         * Constructs a new successful reply.
         *
         * <p>Takes no arguments because the success envelope carries no payload beyond the
         * echoed id.
         */
        public Success() {
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a
         * {@code type="result"} envelope echoing the {@code request} id, as validated by
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebClearDirtyBitsJob",
                exports = "cleanDirtyReplyParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            return Optional.of(new Success());
        }

        /**
         * Compares this reply to another object for equality.
         *
         * <p>All {@link Success} instances are equal because the variant carries no payload.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link Success}, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash code shared by every {@link Success} instance.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Success.class.hashCode();
        }

        /**
         * Returns a debug string identifying this empty success reply.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqClearDirtyBitsResponse.Success[]";
        }
    }

    /**
     * Represents the reply variant signalling that the relay rejected the dirty-bit clear as
     * malformed or unauthorised.
     *
     * <p>Maps to the {@code 4xx} branch of the dirty-bit reply pipeline. The dirty-bit driver
     * should treat the resource as still dirty, because the relay-side marker was not cleared,
     * but should not retry the same request because the failure is structural.
     */
    @WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
    final class ClientError implements IqClearDirtyBitsResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a
         * {@code type="error"} envelope echoing the {@code request} id and carrying a
         * {@code <error/>} child whose {@code code} attribute falls in the {@code 4xx} range,
         * per the parsing contract of
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the client-error
         *         schema
         */
        @WhatsAppWebExport(moduleName = "WAWebClearDirtyBitsJob",
                exports = "clearDirtyBits", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for equality.
         *
         * <p>Two replies are equal when both their {@link #errorCode()} and
         * {@link #errorText()} are equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ClientError} with equal code and
         *         text, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the {@link #errorCode()} and {@link #errorText()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string describing this reply's error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqClearDirtyBitsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the reply variant signalling that the relay encountered a transient internal
     * failure while processing the dirty-bit clear.
     *
     * <p>Maps to the {@code 5xx} branch of the dirty-bit reply pipeline. The dirty-bit driver
     * may retry the same request on the next dirty-bit pass since the marker is still set
     * server-side.
     */
    @WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
    final class ServerError implements IqClearDirtyBitsResponse {
        /**
         * Holds the numeric server-side error code from the {@code <error code/>} attribute.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text from the {@code <error text/>}
         * attribute, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply from the parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text, or {@code null} when omitted
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when
         *         the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns a populated {@link Optional} only when the stanza is a
         * {@code type="error"} envelope echoing the {@code request} id and carrying a
         * {@code <error/>} child whose {@code code} attribute falls outside the {@code 4xx}
         * range, per the parsing contract of
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or
         *         {@link Optional#empty()} when the stanza does not match the server-error
         *         schema
         */
        @WhatsAppWebExport(moduleName = "WAWebClearDirtyBitsJob",
                exports = "clearDirtyBits", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this reply to another object for equality.
         *
         * <p>Two replies are equal when both their {@link #errorCode()} and
         * {@link #errorText()} are equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ServerError} with equal code and
         *         text, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the {@link #errorCode()} and {@link #errorText()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string describing this reply's error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqClearDirtyBitsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
