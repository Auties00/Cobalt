package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed reply family for a {@link SmaxGroupsSetDescriptionRequest}.
 *
 * <p>The three permitted variants are {@link Success}, {@link ClientError}, and {@link ServerError}.
 * {@link Success} additionally carries the optional {@code t} timestamp echoed by the relay, which callers use to
 * stamp the local revision row.
 */
public sealed interface SmaxGroupsSetDescriptionResponse extends SmaxStanza.Response
        permits SmaxGroupsSetDescriptionResponse.Success, SmaxGroupsSetDescriptionResponse.ClientError, SmaxGroupsSetDescriptionResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsSetDescriptionResponse} variant in priority order
     * and returns the first that parses cleanly.
     *
     * <p>The variants are tried in the order {@link Success}, {@link ClientError}, {@link ServerError}.
     *
     * @implNote This implementation returns an empty {@link Optional} when the stanza shape matches none of the
     * variants; WA Web throws a parsing failure on the same path, but Cobalt defers the decision to the caller so
     * it can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsSetDescriptionRequest} stanza, used to validate
     *                echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsSetDescriptionRPC",
            exports = "sendSetDescriptionRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsSetDescriptionResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the reply variant emitted when the relay committed the description mutation.
     *
     * <p>{@link #timestamp()} carries the optional {@code t} attribute echoed by the relay (unix epoch seconds);
     * callers use it as the local revision wall-clock when persisting the new description.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSetDescriptionResponseSuccess")
    final class Success implements SmaxGroupsSetDescriptionResponse {
        /**
         * The optional {@code t} timestamp lifted from the IQ envelope (unix epoch seconds).
         */
        private final Long timestamp;

        /**
         * Constructs a {@link Success}.
         *
         * @param timestamp the optional {@code t} timestamp; may be {@code null}
         */
        public Success(Long timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * Returns the optional {@code t} timestamp.
         *
         * <p>The result is empty when the relay omitted the attribute; present values are unix epoch seconds.
         *
         * @return an {@link Optional} carrying the timestamp
         */
        public Optional<Long> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         *
         * <p>The IQ must be a valid {@code type="result"} echo of {@code request}, validated through
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}; the optional {@code t} attribute is captured
         * when present.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSetDescriptionResponseSuccess",
                exports = "parseSetDescriptionResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var t = stanza.getAttributeAsLong("t", null);
            return Optional.of(new Success(t));
        }

        /**
         * Compares this success to {@code obj} for value equality on {@link #timestamp()}.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with the same timestamp
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.timestamp, that.timestamp);
        }

        /**
         * Returns a hash derived from {@link #timestamp()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(timestamp);
        }

        /**
         * Returns a debug string carrying {@link #timestamp()}.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSetDescriptionResponse.Success[timestamp=" + timestamp + ']';
        }
    }

    /**
     * Represents the reply variant emitted when the relay rejected the request envelope as malformed,
     * unauthorised, referencing a non-existent group, or failing the revision-chain check.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSetDescriptionResponseClientError")
    final class ClientError implements SmaxGroupsSetDescriptionResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay.
         */
        private final String errorText;

        /**
         * Constructs a {@link ClientError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from {@code stanza}.
         *
         * <p>The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, and its code and text populate the
         * returned variant.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSetDescriptionResponseClientError",
                exports = "parseSetDescriptionResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ClientError} with identical fields
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
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSetDescriptionResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the reply variant emitted on transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsSetDescriptionResponseServerError")
    final class ServerError implements SmaxGroupsSetDescriptionResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay.
         */
        private final String errorText;

        /**
         * Constructs a {@link ServerError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from {@code stanza}.
         *
         * <p>The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, and its code and text populate the
         * returned variant.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsSetDescriptionResponseServerError",
                exports = "parseSetDescriptionResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ServerError} with identical fields
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
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsSetDescriptionResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
