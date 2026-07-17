package com.github.auties00.cobalt.wire.stanza.smax.voip;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Projects the relay's reply to a {@link SmaxWaitingRoomToggleCallLinkRequest}
 * into a sealed pair of variants.
 *
 * <p>An inbound {@code <ack class="call">} stanza resolves to either
 * {@link Success} (toggle applied) or {@link ClientError} (toggle rejected).
 * The call-link admin UI uses the result to confirm the toggle change persisted
 * and surfaces a {@link ClientError} as a backend error.
 */
public sealed interface SmaxWaitingRoomToggleCallLinkResponse extends SmaxStanza.Response
        permits SmaxWaitingRoomToggleCallLinkResponse.Success, SmaxWaitingRoomToggleCallLinkResponse.ClientError {

    /**
     * Parses an inbound stanza into the first matching reply variant.
     *
     * <p>The {@link Success} parser is tried first, then {@link ClientError}.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} on no match, where
     * the WA Web dispatcher would instead throw an {@code SmaxParsingFailure}.
     *
     * @param stanza    the inbound stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty on no match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxVoipWaitingRoomToggleCallLinkRPC",
            exports = "sendWaitingRoomToggleCallLinkRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxWaitingRoomToggleCallLinkResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return ClientError.of(stanza, request);
    }

    /**
     * Validates the {@code <ack class="call">} envelope common to both reply
     * variants.
     *
     * <p>The envelope matches when the stanza has the {@code <ack>} description,
     * the {@code class="call"} marker, an {@code id} echoing the request's
     * {@code id}, and the literal {@code from="call"} server.
     *
     * @param stanza    the inbound stanza
     * @param request the original outbound request
     * @return {@code true} when the envelope matches, {@code false} otherwise
     */
    private static boolean validateAckEnvelope(Stanza stanza, Stanza request) {
        if (!stanza.hasDescription("ack")) {
            return false;
        }
        if (!stanza.hasAttribute("class", "call")) {
            return false;
        }
        var requestId = request.getAttributeAsString("id").orElse(null);
        if (requestId == null) {
            return false;
        }
        if (!stanza.hasAttribute("id", requestId)) {
            return false;
        }
        var from = stanza.getAttributeAsString("from").orElse(null);
        if (from == null || !from.equals(JidServer.call().toString())) {
            return false;
        }
        return true;
    }

    /**
     * The successful reply carrying the link-token whose waiting-room state was
     * toggled.
     *
     * <p>The echoed token confirms which link was affected; the new state is
     * implied by the request's {@link SmaxWaitingRoomToggleCallLinkRequest#waitingRoomToggleEnabled()}
     * since the Ack carries no enabled echo.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInVoipWaitingRoomToggleCallLinkResponseWaitingRoomToggleCallLinkAck")
    final class Success implements SmaxWaitingRoomToggleCallLinkResponse {
        /**
         * The echoed call-link token carried by the inner
         * {@code <waiting_room_toggle>} child's {@code link-token} attribute.
         */
        private final String waitingRoomToggleLinkToken;

        /**
         * Constructs a successful reply.
         *
         * @param waitingRoomToggleLinkToken the echoed token; never {@code null}
         * @throws NullPointerException if {@code waitingRoomToggleLinkToken} is {@code null}
         */
        public Success(String waitingRoomToggleLinkToken) {
            this.waitingRoomToggleLinkToken = Objects.requireNonNull(waitingRoomToggleLinkToken, "waitingRoomToggleLinkToken cannot be null");
        }

        /**
         * Returns the echoed call-link token.
         *
         * @return the token; never {@code null}
         */
        public String waitingRoomToggleLinkToken() {
            return waitingRoomToggleLinkToken;
        }

        /**
         * Parses an inbound stanza into a {@link Success} variant.
         *
         * @implNote
         * This implementation requires the shared ack envelope, the
         * {@code type="waiting_room_toggle"} marker, an inner
         * {@code <waiting_room_toggle>} child, and a non-null {@code link-token}
         * attribute.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInVoipWaitingRoomToggleCallLinkResponseWaitingRoomToggleCallLinkAck",
                exports = "parseWaitingRoomToggleCallLinkResponseWaitingRoomToggleCallLinkAck",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!validateAckEnvelope(stanza, request)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "waiting_room_toggle")) {
                return Optional.empty();
            }
            var toggle = stanza.getChild("waiting_room_toggle").orElse(null);
            if (toggle == null) {
                return Optional.empty();
            }
            var linkToken = toggle.getAttributeAsString("link-token").orElse(null);
            if (linkToken == null) {
                return Optional.empty();
            }
            return Optional.of(new Success(linkToken));
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with equal
         *         fields, {@code false} otherwise
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
            return Objects.equals(this.waitingRoomToggleLinkToken, that.waitingRoomToggleLinkToken);
        }

        /**
         * Returns a hash code derived from every field of this reply.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(waitingRoomToggleLinkToken);
        }

        /**
         * Returns a debug string listing every field of this reply.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "SmaxWaitingRoomToggleCallLinkResponse.Success[waitingRoomToggleLinkToken="
                    + waitingRoomToggleLinkToken + ']';
        }
    }

    /**
     * The client-error reply produced when the relay rejects the toggle.
     *
     * <p>Typical causes are a non-creator caller, a revoked link, or a media
     * mismatch; the per-RPC {@link #errorLinkToken()} echoes the offending token
     * to support multi-link admin surfaces.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInVoipWaitingRoomToggleCallLinkResponseWaitingRoomToggleCallLinkNack")
    final class ClientError implements SmaxWaitingRoomToggleCallLinkResponse {
        /**
         * The numeric error code parsed from the {@code error} attribute, or
         * {@code -1} when the raw value is non-numeric.
         */
        private final int errorCode;

        /**
         * The raw {@code error} attribute value.
         */
        private final String errorText;

        /**
         * The per-RPC link-token carried by the inner {@code <error>} child's
         * {@code link-token} attribute.
         *
         * <p>Identifies which link the error refers to.
         */
        private final String errorLinkToken;

        /**
         * Constructs a client-error reply.
         *
         * @param errorCode      the numeric code; {@code -1} when the raw attribute is non-numeric
         * @param errorText      the raw error string; may be {@code null}
         * @param errorLinkToken the per-RPC link-token; may be {@code null}
         */
        public ClientError(int errorCode, String errorText, String errorLinkToken) {
            this.errorCode = errorCode;
            this.errorText = errorText;
            this.errorLinkToken = errorLinkToken;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code, or {@code -1} when the raw {@code error}
         *         attribute was non-numeric
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional raw error string.
         *
         * @return an {@link Optional} carrying the error string, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Returns the per-RPC link-token.
         *
         * @return an {@link Optional} carrying the link-token, or empty when omitted
         */
        public Optional<String> errorLinkToken() {
            return Optional.ofNullable(errorLinkToken);
        }

        /**
         * Parses an inbound stanza into a {@link ClientError} variant.
         *
         * @implNote
         * This implementation requires the shared ack envelope, the
         * {@code type="waiting_room_toggle"} marker, a non-null {@code error}
         * attribute, and a non-null {@code link-token} attribute on the inner
         * {@code <error>} child; the {@code error} value is parsed as a decimal
         * integer and falls back to {@code -1} when non-numeric.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInVoipWaitingRoomToggleCallLinkResponseWaitingRoomToggleCallLinkNack",
                exports = "parseWaitingRoomToggleCallLinkResponseWaitingRoomToggleCallLinkNack",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            if (!validateAckEnvelope(stanza, request)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "waiting_room_toggle")) {
                return Optional.empty();
            }
            var errorAttr = stanza.getAttributeAsString("error").orElse(null);
            if (errorAttr == null) {
                return Optional.empty();
            }
            var errorChild = stanza.getChild("error").orElse(null);
            if (errorChild == null) {
                return Optional.empty();
            }
            var linkToken = errorChild.getAttributeAsString("link-token").orElse(null);
            if (linkToken == null) {
                return Optional.empty();
            }
            int code;
            try {
                code = Integer.parseInt(errorAttr);
            } catch (NumberFormatException _) {
                code = -1;
            }
            return Optional.of(new ClientError(code, errorAttr, linkToken));
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with
         *         equal fields, {@code false} otherwise
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
                    && Objects.equals(this.errorText, that.errorText)
                    && Objects.equals(this.errorLinkToken, that.errorLinkToken);
        }

        /**
         * Returns a hash code derived from every field of this reply.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText, errorLinkToken);
        }

        /**
         * Returns a debug string listing every field of this reply.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "SmaxWaitingRoomToggleCallLinkResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText
                    + ", errorLinkToken=" + errorLinkToken + ']';
        }
    }
}
