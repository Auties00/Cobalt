package com.github.auties00.cobalt.wire.stanza.smax.qp;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Models the outbound {@code <ack class="notification">} stanza that acknowledges a server-pushed
 * {@link SmaxQpQpNotificationResponse}.
 *
 * <p>This request is sent back through the socket pipeline after a quick-promotion (QP) surfaces
 * notification has been consumed. It echoes the inbound {@code id}, {@code from}, and {@code type}
 * triple so the relay can match the ack to the original push and stop re-delivering the same
 * notification on the next reconnect. The stanza carries no body; it exists purely to confirm
 * receipt. Callers obtain an instance from {@link #from(SmaxQpQpNotificationResponse)} when they
 * hold a typed projection, or from {@link #from(Stanza)} when they hold only the raw stanza, then
 * dispatch the {@link StanzaBuilder} produced by {@link #toStanza()}.
 *
 * @deprecated not wired: Quick Promotions surfaces have no headless consumer.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxOutQpSurfacesQPNotificationResponseAck")
@WhatsAppWebModule(moduleName = "WASmaxOutQpSurfacesNotificationClientAckMixin")
public final class SmaxQpQpNotificationAcknowledgement implements SmaxStanza.Request {
    /**
     * Holds the {@code id} of the notification being acknowledged.
     *
     * <p>This value is echoed verbatim into the ack's {@code id} attribute so the relay can pair
     * the acknowledgement with the originating push.
     */
    private final String notificationId;

    /**
     * Holds the {@code from} of the notification being acknowledged.
     *
     * <p>This value becomes the ack's {@code to} attribute, addressing the acknowledgement back to
     * the sender of the original notification.
     */
    private final Jid notificationFrom;

    /**
     * Holds the {@code type} of the notification being acknowledged.
     *
     * <p>This value is echoed verbatim into the ack's {@code type} attribute.
     */
    private final String notificationType;

    /**
     * Constructs an acknowledgement from the three echoed notification attributes.
     *
     * <p>This raw constructor lets the ack be built directly from a captured stanza without
     * round-tripping through the parser. Callers that already hold a typed
     * {@link SmaxQpQpNotificationResponse} should prefer {@link #from(SmaxQpQpNotificationResponse)}
     * instead.
     *
     * @param notificationId the notification id; never {@code null}
     * @param notificationFrom the notification's sender JID; never {@code null}
     * @param notificationType the notification type; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxQpQpNotificationAcknowledgement(String notificationId, Jid notificationFrom, String notificationType) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
    }

    /**
     * Lifts the three echoed attributes off the given typed notification into a fresh
     * acknowledgement.
     *
     * <p>This is the preferred factory once a {@link SmaxQpQpNotificationResponse} has been
     * consumed, since it copies the same {@code id}, {@code from}, and {@code type} triple the
     * relay expects to see reflected in the ack.
     *
     * @param inbound the inbound notification; never {@code null}
     * @return a new acknowledgement
     * @throws NullPointerException if {@code inbound} is {@code null}
     */
    public static SmaxQpQpNotificationAcknowledgement from(SmaxQpQpNotificationResponse inbound) {
        Objects.requireNonNull(inbound, "inbound cannot be null");
        return new SmaxQpQpNotificationAcknowledgement(inbound.notificationId(),
                inbound.notificationFrom(), inbound.notificationType());
    }

    /**
     * Lifts the three echoed attributes off the given raw notification stanza into a fresh
     * acknowledgement.
     *
     * <p>This factory serves consumers that hold a raw {@link Stanza} but not a typed
     * {@link SmaxQpQpNotificationResponse}. The {@code id}, {@code from}, and {@code type}
     * attributes are read verbatim; a missing one raises {@link IllegalArgumentException} rather
     * than being silently absorbed, because the relay requires every ack to round-trip the full
     * triple.
     *
     * @param notification the inbound notification stanza; never {@code null}
     * @return a new acknowledgement
     * @throws NullPointerException if {@code notification} is {@code null}
     * @throws IllegalArgumentException if the notification is missing one of the required echoed
     *                                  attributes
     */
    public static SmaxQpQpNotificationAcknowledgement from(Stanza notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        var id = notification.getAttributeAsString("id")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing id attribute"));
        var from = notification.getAttributeAsJid("from")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing from attribute"));
        var type = notification.getAttributeAsString("type")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing type attribute"));
        return new SmaxQpQpNotificationAcknowledgement(id, from, type);
    }

    /**
     * Returns the notification id being acknowledged.
     *
     * @return the id; never {@code null}
     */
    public String notificationId() {
        return notificationId;
    }

    /**
     * Returns the notification sender JID.
     *
     * @return the sender JID; never {@code null}
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
     * Builds the outbound ack stanza ready for dispatch.
     *
     * <p>The produced builder describes {@code <ack id to class="notification" type/>}. The stanza
     * is fire-and-forget: no reply is expected and the envelope carries no body. The notification's
     * sender JID is written into the {@code to} attribute via
     * {@link StanzaBuilder#attribute(String, com.github.auties00.cobalt.wire.core.jid.JidProvider)}.
     *
     * @return a {@link StanzaBuilder} carrying the ack envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutQpSurfacesQPNotificationResponseAck",
            exports = "makeQPNotificationResponseAck",
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
     * Returns whether the given object is a {@link SmaxQpQpNotificationAcknowledgement} with equal
     * echoed attributes.
     *
     * <p>Two acknowledgements are equal when their {@code id}, {@code from}, and {@code type} fields
     * all match.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when all three fields match
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxQpQpNotificationAcknowledgement) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType);
    }

    /**
     * Returns a hash code derived from the three echoed attributes.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType);
    }

    /**
     * Returns a debug-friendly textual representation of this acknowledgement.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxQpQpNotificationAcknowledgement[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType + ']';
    }
}
