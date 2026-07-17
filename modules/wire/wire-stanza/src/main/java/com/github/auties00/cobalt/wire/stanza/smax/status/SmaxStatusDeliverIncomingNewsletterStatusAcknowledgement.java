package com.github.auties00.cobalt.wire.stanza.smax.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;

/**
 * The outbound {@code <ack class="status">} stanza the client emits to confirm an inbound
 * newsletter-status delivery.
 *
 * <p>This is the reply a caller sends after consuming a delivered
 * {@link SmaxStatusDeliverIncomingNewsletterStatusResponse}. Only a positive {@link SuccessAck}
 * variant exists; the relay does not expose a negative-ack shape for newsletter-status deliveries,
 * unlike newsletter messages which carry a NACK path.
 *
 * @implNote
 * This implementation models the family as a sealed interface with a single implementation so the
 * shape stays symmetric with the sibling
 * {@link com.github.auties00.cobalt.wire.stanza.smax.message.SmaxMessageDeliverNewsletterAcknowledgement}
 * even though no negative-ack variant exists yet.
 *
 * @deprecated superseded by the general newsletter-status path ({@code NewsletterStatusStreamHandler}).
 */
@Deprecated
public sealed interface SmaxStatusDeliverIncomingNewsletterStatusAcknowledgement extends SmaxStanza.Request
        permits SmaxStatusDeliverIncomingNewsletterStatusAcknowledgement.SuccessAck {

    /**
     * The positive ack variant the client emits after consuming a newsletter-status delivery.
     *
     * <p>The relay treats this stanza as a delivery receipt for the inbound status it correlates
     * to via the stanza id, sender, and type.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutStatusDeliverIncomingNewsletterStatusResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxOutStatusDeliverStatusAckMixin")
    final class SuccessAck implements SmaxStatusDeliverIncomingNewsletterStatusAcknowledgement {
        /**
         * The {@code id} of the inbound status being acknowledged.
         */
        private final String stanzaId;

        /**
         * The {@code from} of the inbound status, used as the ack's {@code to}.
         */
        private final Jid notificationFrom;

        /**
         * The {@code type} of the inbound status, echoed back as the ack's {@code type}.
         */
        private final String stanzaType;

        /**
         * Constructs a positive ack carrying the inbound stanza's correlation triplet.
         *
         * @param stanzaId         the inbound status id; never {@code null}
         * @param notificationFrom the inbound sender JID; never {@code null}
         * @param stanzaType       the inbound status type; never {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        public SuccessAck(String stanzaId, Jid notificationFrom, String stanzaType) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
            this.stanzaType = Objects.requireNonNull(stanzaType, "stanzaType cannot be null");
        }

        /**
         * Returns the inbound status id this ack confirms.
         *
         * @return the id; never {@code null}
         */
        public String stanzaId() {
            return stanzaId;
        }

        /**
         * Returns the inbound sender JID this ack is routed back to.
         *
         * @return the JID; never {@code null}
         */
        public Jid notificationFrom() {
            return notificationFrom;
        }

        /**
         * Returns the inbound status type this ack echoes.
         *
         * @return the type; never {@code null}
         */
        public String stanzaType() {
            return stanzaType;
        }

        /**
         * Builds the outbound {@code <ack class="status">} stanza ready for dispatch.
         *
         * <p>The stanza has shape
         * {@snippet lang=xml :
         * <ack to="<notificationFrom>" class="status" id="<stanzaId>" type="<stanzaType>"/>
         * }
         *
         * @return a {@link StanzaBuilder} carrying the ack stanza
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutStatusDeliverIncomingNewsletterStatusResponseSuccess",
                exports = "makeIncomingNewsletterStatusResponseSuccess",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public StanzaBuilder toStanza() {
            return new StanzaBuilder()
                    .description("ack")
                    .attribute("to", notificationFrom)
                    .attribute("class", "status")
                    .attribute("id", stanzaId)
                    .attribute("type", stanzaType);
        }

        /**
         * Compares this ack to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link SuccessAck} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SuccessAck) obj;
            return Objects.equals(this.stanzaId, that.stanzaId)
                    && Objects.equals(this.notificationFrom, that.notificationFrom)
                    && Objects.equals(this.stanzaType, that.stanzaType);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(stanzaId, notificationFrom, stanzaType);
        }

        /**
         * Returns a debug-friendly representation of this ack.
         *
         * <p>The format is intended for logging and is not part of any contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxStatusDeliverIncomingNewsletterStatusAcknowledgement.SuccessAck[stanzaId="
                    + stanzaId + ", notificationFrom=" + notificationFrom
                    + ", stanzaType=" + stanzaType + ']';
        }
    }
}
