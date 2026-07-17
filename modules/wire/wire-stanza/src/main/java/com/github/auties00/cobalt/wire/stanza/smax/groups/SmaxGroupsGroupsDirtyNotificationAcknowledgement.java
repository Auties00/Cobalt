package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * The outbound {@code <ack class="notification">} stanza that confirms receipt of a
 * {@link SmaxGroupsGroupsDirtyNotificationResponse}.
 * <p>
 * The client acks the dirty notification after re-querying the affected groups; emitting the ack stops the relay from
 * re-pushing the same {@code <notification type="w:gp2"/>} on the next socket reconnection. Build one via
 * {@link #from(Stanza)} when the inbound notification is in hand.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGroupsDirtyNotificationResponseAck")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsNotificationClientAckMixin")
public final class SmaxGroupsGroupsDirtyNotificationAcknowledgement implements SmaxStanza.Request {
    /**
     * The {@code id} attribute of the notification being acknowledged.
     */
    private final String notificationId;

    /**
     * The {@code from} attribute of the notification, routed verbatim into the ack's {@code to}.
     */
    private final Jid notificationFrom;

    /**
     * The {@code type} attribute of the notification echoed back into the ack.
     */
    private final String notificationType;

    /**
     * Constructs an acknowledgement from raw attribute values.
     * <p>
     * Prefer {@link #from(Stanza)} when the inbound notification stanza is available; it lifts every required attribute
     * in one step.
     *
     * @param notificationId   the notification id; never {@code null}
     * @param notificationFrom the notification sender {@link Jid}; never {@code null}
     * @param notificationType the notification type; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxGroupsGroupsDirtyNotificationAcknowledgement(String notificationId, Jid notificationFrom, String notificationType) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
    }

    /**
     * Constructs an acknowledgement from a parsed inbound notification.
     * <p>
     * Lifts the {@code id}, {@code from}, and {@code type} attributes verbatim from the supplied
     * {@code <notification/>} stanza.
     *
     * @param notification the inbound notification stanza; never {@code null}
     * @return a new {@link SmaxGroupsGroupsDirtyNotificationAcknowledgement}
     * @throws NullPointerException     if {@code notification} is {@code null}
     * @throws IllegalArgumentException if the notification is missing one of the required echoed attributes
     */
    public static SmaxGroupsGroupsDirtyNotificationAcknowledgement from(Stanza notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        var id = notification.getAttributeAsString("id")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing id attribute"));
        var from = notification.getAttributeAsJid("from")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing from attribute"));
        var type = notification.getAttributeAsString("type")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing type attribute"));
        return new SmaxGroupsGroupsDirtyNotificationAcknowledgement(id, from, type);
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
     * @return the sender JID; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the notification type.
     *
     * @return the notification type; never {@code null}
     */
    public String notificationType() {
        return notificationType;
    }

    /**
     * Materialises the outbound ack stanza ready for dispatch.
     * <p>
     * The resulting envelope is
     * {@snippet :
     *     <ack id="<notificationId>" to="<notificationFrom>" class="notification" type="<notificationType>"/>
     * }
     *
     * @return a {@link StanzaBuilder} carrying the ack envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGroupsDirtyNotificationResponseAck",
            exports = "makeGroupsDirtyNotificationResponseAck",
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
     * Compares this acknowledgement to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGroupsDirtyNotificationAcknowledgement} with
     *         identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGroupsDirtyNotificationAcknowledgement) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGroupsDirtyNotificationAcknowledgement[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType + ']';
    }
}
