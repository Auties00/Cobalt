package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Represents the outbound acknowledgement stanza for a
 * {@link SmaxNewslettersLiveUpdatesNotificationResponse}.
 * The receive pipeline emits this after consuming an inbound
 * live-updates notification; the ack must fire even when the
 * notification is rejected or downstream persistence fails, otherwise
 * the relay re-delivers the same notification.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersLiveUpdatesNotificationResponseAck")
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersNotificationClientAckMixin")
public final class SmaxNewslettersLiveUpdatesNotificationAcknowledgement implements SmaxStanza.Request {
    /**
     * Holds the stanza id of the notification being acknowledged.
     */
    private final String notificationId;

    /**
     * Holds the notification sender, lifted from the inbound
     * {@code from} attribute and emitted as the ack's {@code to}
     * attribute.
     */
    private final Jid notificationFrom;

    /**
     * Holds the notification {@code type} echoed back into the ack.
     */
    private final String notificationType;

    /**
     * Constructs an acknowledgement.
     * All three fields are mandatory because the relay matches an ack
     * against the in-flight notification by {@code (id, from, type)};
     * any mismatch leads to retransmission.
     *
     * @param notificationId   the notification id; never {@code null}
     * @param notificationFrom the notification sender {@link Jid}; never {@code null}
     * @param notificationType the notification type (typically {@code "newsletter"}); never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxNewslettersLiveUpdatesNotificationAcknowledgement(String notificationId, Jid notificationFrom, String notificationType) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
    }

    /**
     * Constructs an acknowledgement from an inbound notification stanza.
     * Lifts {@code id}, {@code from}, and {@code type} verbatim from the
     * inbound stanza so the ack never drifts from what the relay sent.
     *
     * @param notification the inbound notification stanza; never {@code null}
     * @return a new acknowledgement
     * @throws NullPointerException     if {@code notification} is {@code null}
     * @throws IllegalArgumentException if the notification is missing one of the required echoed attributes
     */
    public static SmaxNewslettersLiveUpdatesNotificationAcknowledgement from(Stanza notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        var id = notification.getAttributeAsString("id")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing id attribute"));
        var from = notification.getAttributeAsJid("from")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing from attribute"));
        var type = notification.getAttributeAsString("type")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing type attribute"));
        return new SmaxNewslettersLiveUpdatesNotificationAcknowledgement(id, from, type);
    }

    /**
     * Returns the notification id being acknowledged.
     *
     * @return the notification id; never {@code null}
     */
    public String notificationId() {
        return notificationId;
    }

    /**
     * Returns the notification sender {@link Jid}.
     *
     * @return the sender {@link Jid}; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the notification type.
     *
     * @return the type; never {@code null}
     */
    public String notificationType() {
        return notificationType;
    }

    /**
     * Builds the outbound {@code <ack>} stanza.
     * The shape is
     * {@code <ack id="<id>" to="<from>" class="notification" type="<type>"/>}.
     *
     * @implNote This implementation hard-codes {@code class="notification"} because the ack belongs to the notification fan-out lane the relay routes inbound newsletter notifications through.
     * @return a {@link StanzaBuilder} carrying the ack envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutNewslettersLiveUpdatesNotificationResponseAck",
            exports = "makeLiveUpdatesNotificationResponseAck",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("ack")
                .attribute("id", notificationId)
                .attribute("to", notificationFrom)
                .attribute("class", "notification")
                .attribute("type", notificationType);
    }

    /**
     * Compares two acks for value equality on every field.
     *
     * @param obj the reference object to compare against
     * @return {@code true} when {@code obj} is an ack with equal {@link #notificationId()}, {@link #notificationFrom()}, and {@link #notificationType()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxNewslettersLiveUpdatesNotificationAcknowledgement) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType);
    }

    /**
     * Returns the hash code derived from every field.
     *
     * @return the combined hash of every field
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType);
    }

    /**
     * Returns a debug representation including every field.
     *
     * @return a record-like rendering of this ack
     */
    @Override
    public String toString() {
        return "SmaxNewslettersLiveUpdatesNotificationAcknowledgement[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType + ']';
    }
}
