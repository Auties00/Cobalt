package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Models the outbound {@code <ack class="notification" type="link_code_companion_reg"/>}
 * stanza a companion emits after consuming a {@link SmaxMdRefreshCodeNotifyCompanionResponse}.
 *
 * <p>A companion sends exactly one ack per inbound refresh-code notification so the relay marks
 * the notification delivered and stops replaying it. The three echoed attributes ({@code id},
 * {@code to}, {@code type}) are taken from the inbound notification, while {@code class} is fixed
 * to {@code "notification"}. The usual entry point is
 * {@link #from(SmaxMdRefreshCodeNotifyCompanionResponse)}, which derives those fields from an
 * already-parsed projection.
 *
 * @implNote This implementation folds the WA Web notification-ack mixin into the same builder
 * rather than keeping it as a separate merge pass: {@code class} is pinned to the literal
 * {@code "notification"} that the upstream mixin would otherwise merge in.
 *
 * @deprecated companion-pairing notify handled inline by {@code IqStreamHandler}.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxOutMdRefreshCodeNotifyCompanionResponseAck")
@WhatsAppWebModule(moduleName = "WASmaxOutMdNotificationClientAckMixin")
public final class SmaxMdRefreshCodeNotifyCompanionAcknowledgement implements SmaxStanza.Request {
    /**
     * Holds the {@code id} of the inbound notification being acknowledged.
     *
     * <p>Echoed into the outbound {@code <ack id="..."/>} attribute.
     */
    private final String notificationId;

    /**
     * Holds the sender JID of the inbound notification, always the {@code s.whatsapp.net}
     * server domain.
     *
     * <p>Echoed into the outbound {@code <ack to="..."/>} attribute.
     */
    private final Jid notificationFrom;

    /**
     * Holds the {@code type} attribute of the inbound notification, fixed by the link-code
     * pairing flow to {@code "link_code_companion_reg"}.
     *
     * <p>Echoed back into the {@code <ack type="..."/>} attribute.
     */
    private final String notificationType;

    /**
     * Constructs an ack from already-resolved component fields.
     *
     * <p>Most callers use {@link #from(SmaxMdRefreshCodeNotifyCompanionResponse)} to derive the
     * three echoed fields from a parsed notification projection; this constructor is exposed so
     * unit tests can build fixtures directly.
     *
     * @param notificationId   the notification id; never {@code null}
     * @param notificationFrom the notification sender JID; never {@code null}
     * @param notificationType the notification type; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxMdRefreshCodeNotifyCompanionAcknowledgement(String notificationId, Jid notificationFrom, String notificationType) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
    }

    /**
     * Derives the ack from an already-parsed {@link SmaxMdRefreshCodeNotifyCompanionResponse}
     * projection.
     *
     * <p>Copies the inbound {@link SmaxMdRefreshCodeNotifyCompanionResponse#notificationId()}
     * and {@link SmaxMdRefreshCodeNotifyCompanionResponse#notificationFrom()} into the new ack
     * and pins the type to {@code "link_code_companion_reg"}.
     *
     * @implNote This implementation hardcodes the type rather than reading it back from the
     * inbound projection, because {@link SmaxMdRefreshCodeNotifyCompanionResponse#of(Stanza)} only
     * accepts that exact literal.
     *
     * @param inbound the parsed inbound notification projection
     * @return a new acknowledgement
     * @throws NullPointerException if {@code inbound} is {@code null}
     */
    public static SmaxMdRefreshCodeNotifyCompanionAcknowledgement from(SmaxMdRefreshCodeNotifyCompanionResponse inbound) {
        Objects.requireNonNull(inbound, "inbound cannot be null");
        return new SmaxMdRefreshCodeNotifyCompanionAcknowledgement(inbound.notificationId(), inbound.notificationFrom(), "link_code_companion_reg");
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
     * Returns the notification sender JID that becomes the ack's {@code to} attribute.
     *
     * @return the JID; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the {@code type} attribute echoed by the ack.
     *
     * @return the type; never {@code null}
     */
    public String notificationType() {
        return notificationType;
    }

    /**
     * Builds the outbound ack stanza.
     *
     * <p>Returns the unfinished {@link StanzaBuilder} so the dispatch path can stamp the wire-level
     * identifiers before flushing, matching the contract of {@link SmaxStanza.Request#toStanza()}.
     *
     * @implNote This implementation pins {@code class} to the literal {@code "notification"}
     * that the WA Web notification-ack mixin merges into the stanza shape.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <ack>} envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutMdRefreshCodeNotifyCompanionResponseAck",
            exports = "makeRefreshCodeNotifyCompanionResponseAck",
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
     * Compares this acknowledgement to another object for value equality.
     *
     * <p>Two acknowledgements are equal when their notification id, sender JID, and type all
     * match.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal acknowledgement
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdRefreshCodeNotifyCompanionAcknowledgement) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code derived from the notification id, sender JID, and type
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType);
    }

    /**
     * Returns a debug string listing the notification id, sender JID, and type.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdRefreshCodeNotifyCompanionAcknowledgement[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType + ']';
    }
}
