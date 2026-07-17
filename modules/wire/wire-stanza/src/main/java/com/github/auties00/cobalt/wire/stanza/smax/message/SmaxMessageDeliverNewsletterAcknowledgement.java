package com.github.auties00.cobalt.wire.stanza.smax.message;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Closes the set of outbound {@code <ack class="message">} stanzas the client emits to confirm or
 * reject an inbound newsletter delivery.
 *
 * <p>A delivery surfaced as {@link SmaxMessageDeliverNewsletterResponse} is answered with exactly one
 * variant: {@link SuccessAck} when the inbound message decoded and persisted cleanly, or
 * {@link ErrorAck} when it failed to decrypt, was malformed, or did not match the expected schema.
 * The error variant stamps the fixed {@code error="406"} marker so the relay treats the reply as a
 * definitive not-acknowledged signal. Both variants carry the same correlation triplet copied from
 * the inbound stanza: its id, its sender JID (which becomes the ack's {@code to}), and its type.
 *
 * @deprecated superseded by the general newsletter-message decode ({@code MessageStreamHandler}/{@code messageService});
 * too narrow (media-only).
 */
@Deprecated
public sealed interface SmaxMessageDeliverNewsletterAcknowledgement extends SmaxStanza.Request
        permits SmaxMessageDeliverNewsletterAcknowledgement.SuccessAck, SmaxMessageDeliverNewsletterAcknowledgement.ErrorAck {

    /**
     * Represents the positive acknowledgement emitted after a newsletter delivery decoded and
     * persisted cleanly.
     *
     * <p>The relay treats this stanza as a successful delivery receipt for the inbound message named
     * by the carried correlation triplet.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutMessageDeliverNewsletterResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxOutMessageDeliverCommonAckMixin")
    final class SuccessAck implements SmaxMessageDeliverNewsletterAcknowledgement {
        /**
         * Holds the {@code id} of the inbound message being acknowledged.
         */
        private final String stanzaId;

        /**
         * Holds the {@code from} of the inbound message, which becomes the ack's {@code to}.
         */
        private final Jid notificationFrom;

        /**
         * Holds the {@code type} of the inbound message, echoed back as the ack's {@code type}.
         */
        private final String stanzaType;

        /**
         * Constructs a positive ack carrying the inbound stanza's correlation triplet.
         *
         * @param stanzaId         the inbound message id; never {@code null}
         * @param notificationFrom the inbound sender JID; never {@code null}
         * @param stanzaType       the inbound message type; never {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        public SuccessAck(String stanzaId, Jid notificationFrom, String stanzaType) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
            this.stanzaType = Objects.requireNonNull(stanzaType, "stanzaType cannot be null");
        }

        /**
         * Returns the inbound message id this ack confirms.
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
         * Returns the inbound message type this ack echoes.
         *
         * @return the type; never {@code null}
         */
        public String stanzaType() {
            return stanzaType;
        }

        /**
         * Builds the outbound {@code <ack class="message">} stanza ready for dispatch.
         *
         * <p>The resulting stanza has shape
         * {@snippet lang=xml :
         * <ack to="<notificationFrom>" class="message" id="<stanzaId>" type="<stanzaType>"/>
         * }
         *
         * @return a {@link StanzaBuilder} carrying the ack stanza; never {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutMessageDeliverNewsletterResponseSuccess",
                exports = "makeNewsletterResponseSuccess",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public StanzaBuilder toStanza() {
            return new StanzaBuilder()
                    .description("ack")
                    .attribute("to", notificationFrom)
                    .attribute("class", "message")
                    .attribute("id", stanzaId)
                    .attribute("type", stanzaType);
        }

        /**
         * Compares this ack to another object for value equality across every field.
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
         * <p>The format is intended for logging and is not part of any stable contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxMessageDeliverNewsletterAcknowledgement.SuccessAck[stanzaId=" + stanzaId
                    + ", notificationFrom=" + notificationFrom
                    + ", stanzaType=" + stanzaType + ']';
        }
    }

    /**
     * Represents the negative acknowledgement emitted when a newsletter delivery could not be
     * consumed.
     *
     * <p>This variant is used when the inbound message failed to decrypt, was malformed, or did not
     * match the expected schema. The fixed {@code error="406"} marker tells the relay to treat the
     * delivery as definitively not acknowledged.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutMessageDeliverNewsletterResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxOutMessageDeliverCommonAckMixin")
    final class ErrorAck implements SmaxMessageDeliverNewsletterAcknowledgement {
        /**
         * Holds the {@code id} of the inbound message being rejected.
         */
        private final String stanzaId;

        /**
         * Holds the {@code from} of the inbound message, which becomes the ack's {@code to}.
         */
        private final Jid notificationFrom;

        /**
         * Holds the {@code type} of the inbound message, echoed back as the ack's {@code type}.
         */
        private final String stanzaType;

        /**
         * Constructs a negative ack carrying the inbound stanza's correlation triplet.
         *
         * @param stanzaId         the inbound message id; never {@code null}
         * @param notificationFrom the inbound sender JID; never {@code null}
         * @param stanzaType       the inbound message type; never {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        public ErrorAck(String stanzaId, Jid notificationFrom, String stanzaType) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
            this.stanzaType = Objects.requireNonNull(stanzaType, "stanzaType cannot be null");
        }

        /**
         * Returns the inbound message id this ack rejects.
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
         * Returns the inbound message type this ack echoes.
         *
         * @return the type; never {@code null}
         */
        public String stanzaType() {
            return stanzaType;
        }

        /**
         * Builds the outbound {@code <ack error="406">} stanza ready for dispatch.
         *
         * <p>The resulting stanza has shape
         * {@snippet lang=xml :
         * <ack error="406" to="<notificationFrom>" class="message" id="<stanzaId>" type="<stanzaType>"/>
         * }
         *
         * @return a {@link StanzaBuilder} carrying the negative-ack stanza; never {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutMessageDeliverNewsletterResponseError",
                exports = "makeNewsletterResponseError",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public StanzaBuilder toStanza() {
            return new StanzaBuilder()
                    .description("ack")
                    .attribute("error", "406")
                    .attribute("to", notificationFrom)
                    .attribute("class", "message")
                    .attribute("id", stanzaId)
                    .attribute("type", stanzaType);
        }

        /**
         * Compares this ack to another object for value equality across every field.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an {@link ErrorAck} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ErrorAck) obj;
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
         * <p>The format is intended for logging and is not part of any stable contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxMessageDeliverNewsletterAcknowledgement.ErrorAck[stanzaId=" + stanzaId
                    + ", notificationFrom=" + notificationFrom
                    + ", stanzaType=" + stanzaType + ']';
        }
    }
}
