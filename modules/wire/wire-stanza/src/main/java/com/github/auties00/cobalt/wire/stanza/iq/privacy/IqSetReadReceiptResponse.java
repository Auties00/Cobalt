package com.github.auties00.cobalt.wire.stanza.iq.privacy;

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
 * Roots the sealed family of inbound reply variants produced by the relay for an
 * {@link IqSetReadReceiptRequest}.
 *
 * <p>Pattern matching against the three permitted subtypes ({@link Success}, {@link ClientError},
 * {@link ServerError}) surfaces either the echoed read-receipts state or the error envelope.
 *
 * @implNote
 * This implementation collapses WA Web's split parse-and-throw flow into a single sealed
 * hierarchy: WA Web parses the success payload and separately throws a status-code error on a
 * {@code 4xx}/{@code 5xx} reply, whereas Cobalt models both outcomes as permitted subtypes.
 *
 * @deprecated paired with the deprecated {@link IqSetReadReceiptRequest}; read-receipt toggling flows
 * through the multi-row privacy path ({@code editPrivacySetting} via {@link IqSetPrivacyRequest}), so
 * this single-row reply is retained only for source-mapping completeness.
 */
@Deprecated
public sealed interface IqSetReadReceiptResponse extends IqStanza.Response
        permits IqSetReadReceiptResponse.Success, IqSetReadReceiptResponse.ClientError, IqSetReadReceiptResponse.ServerError {

    /**
     * Parses an inbound IQ stanza into the first {@link IqSetReadReceiptResponse} variant that
     * matches.
     *
     * <p>The dispatcher calls this after receiving an inbound {@code <iq>} stanza whose id matches
     * an outstanding {@link IqSetReadReceiptRequest}. An empty {@link Optional} indicates the
     * stanza did not match any documented schema.
     *
     * @implNote
     * This implementation tries {@link Success#of(Stanza, Stanza)} first, then
     * {@link ClientError#of(Stanza, Stanza)}, then {@link ServerError#of(Stanza, Stanza)}, returning the
     * first present result.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza
     * @return the parsed variant, or {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSetReadReceiptJob",
            exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSetReadReceiptResponse> of(Stanza stanza, Stanza request) {
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
     * Represents the successful reply variant in which the relay echoes the new read-receipts
     * category value.
     *
     * <p>Consumers write the echoed {@link #enabled()} state back to the local privacy snapshot.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetReadReceiptJob")
    final class Success implements IqSetReadReceiptResponse {
        /**
         * Holds the new read-receipts state echoed by the relay.
         */
        private final boolean enabled;

        /**
         * Constructs a successful reply for the given echoed state.
         *
         * <p>Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable directly for tests and fixtures.
         *
         * @param enabled the echoed read-receipts state
         */
        public Success(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the echoed read-receipts state.
         *
         * @return {@code true} when read receipts are enabled, {@code false} when disabled
         */
        public boolean enabled() {
            return enabled;
        }

        /**
         * Parses a {@link Success} variant from the given inbound stanza.
         *
         * <p>An empty {@link Optional} signals the stanza is not a success envelope or the
         * embedded category was not the expected {@code readreceipts} row; the caller then falls
         * through to {@link ClientError#of(Stanza, Stanza)} and {@link ServerError#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation first asserts the {@code <iq type="result">} envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, then walks
         * {@code <privacy>/<category>} and verifies the {@code name} attribute is
         * {@code "readreceipts"}. Only the literal {@code "all"} and {@code "none"} values resolve
         * to {@code true} and {@code false}; any other value maps to {@link Optional#empty()},
         * which the caller treats as a schema mismatch and falls through to the error variants.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the stanza does not match
         *         the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetReadReceiptJob",
                exports = "photoResponseParser",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var privacy = stanza.getChild("privacy").orElse(null);
            if (privacy == null) {
                return Optional.empty();
            }
            var category = privacy.getChild("category").orElse(null);
            if (category == null) {
                return Optional.empty();
            }
            var name = category.getAttributeAsString("name").orElse(null);
            if (!"readreceipts".equals(name)) {
                return Optional.empty();
            }
            var value = category.getAttributeAsString("value").orElse(null);
            if (value == null) {
                return Optional.empty();
            }
            if ("all".equals(value)) {
                return Optional.of(new Success(true));
            }
            if ("none".equals(value)) {
                return Optional.of(new Success(false));
            }
            return Optional.empty();
        }

        /**
         * Compares this variant to another object for equality by toggle state.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} with the same
         *         {@link #enabled()} state
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
            return this.enabled == that.enabled;
        }

        /**
         * Returns a hash code derived from the toggle state.
         *
         * <p>The result is consistent with {@link #equals(Object)}.
         *
         * @return the hash code for this variant
         */
        @Override
        public int hashCode() {
            return Objects.hash(enabled);
        }

        /**
         * Returns a debug-only string representation of this variant.
         *
         * <p>The format is not stable and must not be parsed.
         *
         * @return a debug string describing the toggle state
         */
        @Override
        public String toString() {
            return "IqSetReadReceiptResponse.Success[enabled=" + enabled + ']';
        }
    }

    /**
     * Represents the client-error reply variant in which the relay rejected the request with a
     * {@code 4xx} error code.
     *
     * <p>The {@code <error code=... text=.../>} envelope is surfaced as typed fields so the caller
     * can decide whether to retry, escalate, or surface the failure to the UI.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetReadReceiptJob")
    final class ClientError implements IqSetReadReceiptResponse {
        /**
         * Holds the numeric server-side error code, typically in the {@code 4xx} range.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed back by the relay; may be
         * {@code null}.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply for the given code and text.
         *
         * <p>Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable directly for tests and fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ClientError} variant from the given inbound stanza.
         *
         * @implNote
         * This implementation delegates the envelope match to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the schema does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebSetReadReceiptJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality by error code and text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link ClientError} with the same
         *         {@link #errorCode()} and {@link #errorText()}
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
         * Returns a hash code derived from the error code and text.
         *
         * <p>The result is consistent with {@link #equals(Object)}.
         *
         * @return the hash code for this variant
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-only string representation of this variant.
         *
         * <p>The format is not stable and must not be parsed.
         *
         * @return a debug string describing the error code and text
         */
        @Override
        public String toString() {
            return "IqSetReadReceiptResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Represents the server-error reply variant in which the relay encountered a transient
     * internal failure, signalled by a {@code 5xx} error code.
     *
     * <p>This variant is distinguished from {@link ClientError} so callers can choose a different
     * retry policy.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetReadReceiptJob")
    final class ServerError implements IqSetReadReceiptResponse {
        /**
         * Holds the numeric server-side error code, typically in the {@code 5xx} range.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed back by the relay; may be
         * {@code null}.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply for the given code and text.
         *
         * <p>Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable directly for tests and fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a {@link ServerError} variant from the given inbound stanza.
         *
         * @implNote
         * This implementation delegates the envelope match to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the schema does not match
         */
        @WhatsAppWebExport(moduleName = "WAWebSetReadReceiptJob",
                exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality by error code and text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link ServerError} with the same
         *         {@link #errorCode()} and {@link #errorText()}
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
         * Returns a hash code derived from the error code and text.
         *
         * <p>The result is consistent with {@link #equals(Object)}.
         *
         * @return the hash code for this variant
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug-only string representation of this variant.
         *
         * <p>The format is not stable and must not be parsed.
         *
         * @return a debug string describing the error code and text
         */
        @Override
        public String toString() {
            return "IqSetReadReceiptResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
