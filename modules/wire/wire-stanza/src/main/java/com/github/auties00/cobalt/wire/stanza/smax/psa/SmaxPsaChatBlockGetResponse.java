package com.github.auties00.cobalt.wire.stanza.smax.psa;

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
 * Models the inbound reply to a {@link SmaxPsaChatBlockGetRequest} as a
 * sealed disjunction of its documented variants.
 *
 * <p>A reply resolves to either the {@link Success} variant, which carries
 * the current PSA {@link SmaxPsaChatBlockGetBlockingStatus}, or the
 * {@link ServerError} variant, which collapses the documented {@code 5xx} and
 * {@code 4xx} error envelopes into a single {@code (errorCode, errorText)}
 * pair. A successful reply feeds the local PSA-muted preference; an error
 * reply surfaces the relay's reason.
 */
public sealed interface SmaxPsaChatBlockGetResponse extends SmaxStanza.Response
        permits SmaxPsaChatBlockGetResponse.Success, SmaxPsaChatBlockGetResponse.ServerError {

    /**
     * Parses an inbound stanza into the first matching reply variant.
     *
     * <p>Attempts {@link Success#of(Stanza, Stanza)} first and falls back to
     * {@link ServerError#of(Stanza, Stanza)}, returning {@link Optional#empty()}
     * when neither variant matches so the caller can route the parse miss
     * through the configured error policy.
     *
     * @implNote
     * This implementation returns an empty {@link Optional} on no-match
     * rather than throwing, diverging from the WA Web parser that raises a
     * {@code SmaxParsingFailure}.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty on no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPsaChatBlockGetRPC",
            exports = "sendChatBlockGetRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxPsaChatBlockGetResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Models the successful reply carrying the current PSA blocking status.
     *
     * <p>The {@link #blockingStatus()} value reflects the server-side mute
     * state of the PSA broadcast channel, which callers project into the
     * local PSA-muted preference.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPsaChatBlockGetResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInPsaIQResultResponseMixin")
    final class Success implements SmaxPsaChatBlockGetResponse {
        /**
         * Holds the current blocking status echoed by the relay, either
         * {@link SmaxPsaChatBlockGetBlockingStatus#BLOCKED} or
         * {@link SmaxPsaChatBlockGetBlockingStatus#UNBLOCKED}.
         */
        private final SmaxPsaChatBlockGetBlockingStatus blockingStatus;

        /**
         * Constructs a successful reply around the given blocking status.
         *
         * @param blockingStatus the blocking status; never {@code null}
         * @throws NullPointerException if {@code blockingStatus} is {@code null}
         */
        public Success(SmaxPsaChatBlockGetBlockingStatus blockingStatus) {
            this.blockingStatus = Objects.requireNonNull(blockingStatus, "blockingStatus cannot be null");
        }

        /**
         * Returns the current blocking status echoed by the relay.
         *
         * @return the blocking status; never {@code null}
         */
        public SmaxPsaChatBlockGetBlockingStatus blockingStatus() {
            return blockingStatus;
        }

        /**
         * Parses an inbound stanza into a {@link Success} variant.
         *
         * <p>Validates the shared
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} result
         * envelope, then requires an inner {@code <blocking>} child whose
         * {@code status} attribute resolves through
         * {@link SmaxPsaChatBlockGetBlockingStatus#ofWire(String)}. Any
         * missing or unrecognised element yields {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPsaChatBlockGetResponseSuccess",
                exports = "parseChatBlockGetResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var blocking = stanza.getChild("blocking").orElse(null);
            if (blocking == null) {
                return Optional.empty();
            }
            var statusAttr = blocking.getAttributeAsString("status").orElse(null);
            if (statusAttr == null) {
                return Optional.empty();
            }
            var status = SmaxPsaChatBlockGetBlockingStatus.ofWire(statusAttr).orElse(null);
            if (status == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(status));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return this.blockingStatus == that.blockingStatus;
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockingStatus);
        }

        @Override
        public String toString() {
            return "SmaxPsaChatBlockGetResponse.Success[blockingStatus=" + blockingStatus + ']';
        }
    }

    /**
     * Models the server-error reply collapsing the documented PSA error
     * envelopes into a single {@code (errorCode, errorText)} pair.
     *
     * @implNote
     * This implementation flattens the four WA Web error mixins
     * ({@code 500}, {@code 408}, {@code 503}, {@code 429}) into one pair
     * because the surfaced behaviour consumes only the numeric code and the
     * optional message, so the per-mixin distinction carries no information.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPsaChatBlockGetResponseServerError")
    @WhatsAppWebModule(moduleName = "WASmaxInPsaChatBlockError")
    final class ServerError implements SmaxPsaChatBlockGetResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text supplied by the relay.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply around the given code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text supplied by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses an inbound stanza into a {@link ServerError} variant.
         *
         * <p>Delegates to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to
         * validate the IQ-error envelope and extract the {@code (code, text)}
         * pair, returning {@link Optional#empty()} when the stanza does not
         * match a server-error envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPsaChatBlockGetResponseServerError",
                exports = "parseChatBlockGetResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "SmaxPsaChatBlockGetResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
