package com.github.auties00.cobalt.wire.stanza.smax.passivemode;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Closes the family of inbound reply variants for {@link SmaxPassiveModeActiveIQRequest}.
 *
 * <p>The active-mode RPC documents a single valid reply shape, so {@link Success} is the only permitted variant; any
 * other inbound shape is treated as a parsing failure rather than a modelled variant. Code that classifies an inbound
 * stanza into this disjunction only needs to handle the success case.
 */
public sealed interface SmaxPassiveModeActiveIQResponse extends SmaxStanza.Response
        permits SmaxPassiveModeActiveIQResponse.Success {

    /**
     * Lifts an inbound stanza into the sealed disjunction by trying the {@link Success} variant.
     *
     * <p>This is the single entry point used by the SMAX dispatch path to classify an inbound stanza. An empty result
     * signals that the relay did not produce a documented active-mode acknowledgement, which the caller should surface
     * to its error handler.
     *
     * @implNote
     * This implementation delegates straight to {@link Success#of(Stanza, Stanza)} because the RPC has only one valid reply
     * shape.
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxPassiveModeActiveIQRPC",
            exports = "sendActiveIQRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxPassiveModeActiveIQResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        return Success.of(stanza, request);
    }

    /**
     * Projects the echoed {@code <iq type="result" from="s.whatsapp.net">} envelope of a successful active-mode flip.
     *
     * <p>Presence of this variant signals that the relay flipped the connection out of passive mode and will now stream
     * live deliveries to this socket.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPassiveModeActiveIQResponseSuccess")
    final class Success implements SmaxPassiveModeActiveIQResponse {
        /**
         * Holds the {@code from} JID echoed by the relay.
         *
         * <p>This is always {@link JidServer#user()} ({@code s.whatsapp.net}); {@link #of(Stanza, Stanza)} rejects any
         * other server before constructing the variant.
         */
        private final Jid from;

        /**
         * Constructs a successful reply carrying the echoed server JID.
         *
         * <p>Instances are normally obtained through {@link #of(Stanza, Stanza)} after the envelope passes validation
         * rather than constructed directly.
         *
         * @param from the echoed {@code from} JID
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public Success(Jid from) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
        }

        /**
         * Returns the {@code from} JID echoed by the relay.
         *
         * <p>The value identifies the server endpoint that acknowledged the active-mode flip; for normal control flow
         * the mere presence of a {@link Success} instance is enough.
         *
         * @return the {@link Jid}
         */
        public Jid from() {
            return from;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed {@code <iq type="result">} echoing
         * the request id and carrying both a {@code from=s.whatsapp.net} attribute and a nested {@code <active/>}
         * marker. An empty result is a protocol-level rejection.
         *
         * @implNote
         * This implementation checks, in order: tag, type, id echo, {@code <active/>} presence, and {@code from} JID
         * echo. Any failed check short-circuits to {@link Optional#empty()}.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on no-match
         * @throws NullPointerException if either argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInPassiveModeActiveIQResponseSuccess",
                exports = "parseActiveIQResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            Objects.requireNonNull(request, "request cannot be null");
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null || !stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            if (stanza.getChild("active").isEmpty()) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null || !from.hasServer(JidServer.user())) {
                return Optional.empty();
            }
            return Optional.of(new Success(from));
        }

        /**
         * Indicates whether the given object is equal to this reply.
         *
         * <p>Equality is decided solely by the {@link #from} field, which is the only carried attribute.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@code Success} with an equal {@code from} JID
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
            return Objects.equals(this.from, that.from);
        }

        /**
         * Returns a hash code for this reply.
         *
         * <p>The hash derives solely from the {@link #from} field to stay consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from);
        }

        /**
         * Returns the string representation of this reply.
         *
         * <p>The rendering mirrors the record-like form used across the {@code Smax*} response family.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxPassiveModeActiveIQResponse.Success[from=" + from + ']';
        }
    }
}
