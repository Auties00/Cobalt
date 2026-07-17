package com.github.auties00.cobalt.wire.stanza.smax.waffle;

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
 * Models the sealed family of inbound replies to a {@link SmaxWaffleStateExistsRequest}.
 * <p>
 * A reply is exactly one of three shapes: a {@link Success} carrying the {@code wf_state} code and the
 * optional suspended-state marker, a {@link ClientError} for malformed or unauthorised requests (codes below
 * {@code 500}), or a {@link ServerError} for transient relay failures (codes at or above {@code 500}). The
 * success {@code wf_state} code is one of {@code 1} (unlinked), {@code 2} (active), or {@code 3} (paused);
 * any other value is treated as an unknown state.
 */
public sealed interface SmaxWaffleStateExistsResponse extends SmaxStanza.Response
        permits SmaxWaffleStateExistsResponse.Success, SmaxWaffleStateExistsResponse.ClientError, SmaxWaffleStateExistsResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching variant.
     * <p>
     * The stanza is offered to the {@link Success} parser first, then the {@link ClientError} parser, then
     * the {@link ServerError} parser; the first that parses cleanly wins. An empty {@link Optional} means
     * none of the three matched.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when none of the three parsers matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxWaffleStateExistsRPC",
            exports = "sendStateExistsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxWaffleStateExistsResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success reply: the relay reported the current Waffle state code and, optionally, the
     * suspended-state marker.
     * <p>
     * The {@link #wfState()} code is one of {@code 1} (unlinked), {@code 2} (active), or {@code 3} (paused).
     * The optional suspended-state marker surfaces only when the account is currently suspended, and carries
     * an inner no-personal-recovery flag (exposed by {@link #suspendedNpr()}) when the suspension blocks
     * personal-recovery flows.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleStateExistsResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleIQResultResponseMixin")
    final class Success implements SmaxWaffleStateExistsResponse {
        /**
         * Holds the {@code <wf_state/>} integer.
         */
        private final int wfState;

        /**
         * Holds the suspended-state no-personal-recovery flag, or {@code null} when the attribute is absent.
         */
        private final Boolean suspendedNpr;

        /**
         * Holds whether the relay surfaced an explicit suspended-state child.
         */
        private final boolean suspended;

        /**
         * Constructs a success reply from the state code, suspension flag, and no-personal-recovery flag.
         *
         * @param wfState      the {@code <wf_state/>} integer
         * @param suspended    {@code true} when a suspended-state child was present
         * @param suspendedNpr the inner no-personal-recovery flag, or {@code null} when absent
         */
        public Success(int wfState, boolean suspended, Boolean suspendedNpr) {
            this.wfState = wfState;
            this.suspended = suspended;
            this.suspendedNpr = suspendedNpr;
        }

        /**
         * Returns the Waffle state code.
         * <p>
         * The code is one of {@code 1} (unlinked), {@code 2} (active), or {@code 3} (paused); any other value
         * is treated as an unrecognised state.
         *
         * @return the state code as supplied by the relay
         */
        public int wfState() {
            return wfState;
        }

        /**
         * Reports whether the relay surfaced an explicit suspended-state child.
         *
         * @return {@code true} when the account is reported as suspended
         */
        public boolean suspended() {
            return suspended;
        }

        /**
         * Returns the no-personal-recovery flag when the suspended-state child carried one.
         * <p>
         * An empty {@link Optional} represents the wire absence rather than {@code false}, so callers can
         * distinguish a flag that was not surfaced from a flag that was explicitly false.
         *
         * @return an {@link Optional} carrying the flag, or empty when absent
         */
        public Optional<Boolean> suspendedNpr() {
            return Optional.ofNullable(suspendedNpr);
        }

        /**
         * Parses a success variant from the inbound stanza.
         * <p>
         * Returns an empty {@link Optional} when the envelope check fails, when the {@code <wf_state/>} child
         * is missing or non-numeric, or when the optional suspended-state child is malformed.
         *
         * @implNote This implementation parses the {@code wf_state} content as an ASCII integer after a
         * {@link String#trim()} pass, which is more permissive about surrounding whitespace than WhatsApp
         * Web's typed content parser. The no-personal-recovery flag is matched case-insensitively against the
         * literal {@code "true"}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleStateExistsResponseSuccess",
                exports = "parseStateExistsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var wfStateNode = stanza.getChild("wf_state").orElse(null);
            if (wfStateNode == null) {
                return Optional.empty();
            }
            var wfState = wfStateNode.toContentString()
                    .map(String::trim)
                    .map(s -> {
                        try {
                            return Integer.parseInt(s);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .orElse(null);
            if (wfState == null) {
                return Optional.empty();
            }
            var suspendedNode = stanza.getChild("suspended_state").orElse(null);
            Boolean npr = null;
            if (suspendedNode != null) {
                var nprValue = suspendedNode.getAttributeAsString("npr").orElse(null);
                if (nprValue != null) {
                    npr = "true".equalsIgnoreCase(nprValue);
                }
            }
            return Optional.of(new Success(wfState, suspendedNode != null, npr));
        }

        /**
         * Returns whether the given object is a {@link Success} with equal payload fields.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when state, suspension flag, and no-personal-recovery flag all match
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
            return this.wfState == that.wfState
                    && this.suspended == that.suspended
                    && Objects.equals(this.suspendedNpr, that.suspendedNpr);
        }

        /**
         * Returns a hash code derived from the three payload fields.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(wfState, suspended, suspendedNpr);
        }

        /**
         * Returns a debug rendering of this success variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleStateExistsResponse.Success[wfState=" + wfState
                    + ", suspended=" + suspended
                    + ", suspendedNpr=" + suspendedNpr + ']';
        }
    }

    /**
     * Models the client-error reply: the relay rejected the request with a code below {@code 500}.
     * <p>
     * The carried {@link #errorCode()} and {@link #errorText()} are the relay-reported failure details; the
     * error text is the signal embedders typically consume, while the code is preserved for telemetry.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleStateExistsResponseError")
    final class ClientError implements SmaxWaffleStateExistsResponse {
        /**
         * Holds the numeric error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay-reported code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when absent
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code as supplied by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a client-error variant from the inbound stanza.
         * <p>
         * The envelope and code-range check is delegated to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which only matches codes below
         * {@code 500}; codes at or above that threshold fall through to {@link ServerError}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleStateExistsResponseError",
                exports = "parseStateExistsResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Returns whether the given object is a {@link ClientError} with equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both code and text match
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
         * Returns a hash code derived from the code and text.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of this client-error variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleStateExistsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error reply: the relay rejected the request with a code of {@code 500} or above.
     * <p>
     * Indicates a transient relay-side failure. The split from {@link ClientError} exists so telemetry can
     * distinguish client-side from server-side regressions; the carried {@link #errorCode()} and
     * {@link #errorText()} are the relay-reported failure details.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInWaffleStateExistsResponseError")
    final class ServerError implements SmaxWaffleStateExistsResponse {
        /**
         * Holds the numeric error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay-reported code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable text, or {@code null} when absent
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code as supplied by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses a server-error variant from the inbound stanza.
         * <p>
         * The envelope and code-range check is delegated to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which only matches codes at or above {@code 500}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInWaffleStateExistsResponseError",
                exports = "parseStateExistsResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Returns whether the given object is a {@link ServerError} with equal code and text.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both code and text match
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
         * Returns a hash code derived from the code and text.
         *
         * @return a content-based hash consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug rendering of this server-error variant.
         *
         * @return a human-readable summary; never {@code null}
         */
        @Override
        public String toString() {
            return "SmaxWaffleStateExistsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
