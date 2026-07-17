package com.github.auties00.cobalt.wire.stanza.smax.psa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;

/**
 * Models the outbound acknowledgement stanza emitted in response to a
 * {@link SmaxPsaResetSmbLastQpPrefetchTimestampResponse} notification.
 *
 * <p>Sending this ack closes the loop on the SMB quick-promotions
 * prefetch-timestamp reset: it confirms receipt of the server-pushed
 * notification by echoing its {@code id}, {@code from}, and {@code type} back
 * to the relay. The actual quick-promotion refresh is a separate concern
 * triggered by the notification handler, not by this ack.
 *
 * @deprecated not wired: SMB QP-prefetch reset has no headless consumer.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxOutPsaResetSmbLastQpPrefetchTimestampResponseAck")
@WhatsAppWebModule(moduleName = "WASmaxOutPsaNotificationClientAckMixin")
public final class SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement implements SmaxStanza.Request {
    /**
     * Holds the notification id being acknowledged, echoed verbatim into the
     * ack stanza's {@code id} attribute.
     */
    private final String notificationId;

    /**
     * Holds the notification sender JID, echoed into the ack stanza's
     * {@code to} attribute.
     */
    private final Jid notificationFrom;

    /**
     * Holds the notification type echoed into the ack stanza's {@code type}
     * attribute, always {@code "psa"} for this RPC.
     */
    private final String notificationType;

    /**
     * Constructs an acknowledgement around the given echoed attributes.
     *
     * @param notificationId   the notification id; never {@code null}
     * @param notificationFrom the notification sender JID; never {@code null}
     * @param notificationType the notification type; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement(String notificationId, Jid notificationFrom, String notificationType) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
    }

    /**
     * Constructs an acknowledgement by lifting the echoed attributes from a
     * parsed inbound notification stanza.
     *
     * <p>Reads the {@code id}, {@code from}, and {@code type} attributes from
     * {@code notification} verbatim, throwing
     * {@link IllegalArgumentException} when any of the three is absent.
     *
     * @param notification the inbound notification stanza; never {@code null}
     * @return a new acknowledgement
     * @throws NullPointerException     if {@code notification} is {@code null}
     * @throws IllegalArgumentException if the notification is missing one of
     *                                  the required echoed attributes
     */
    public static SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement from(Stanza notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        var id = notification.getAttributeAsString("id")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing id attribute"));
        var from = notification.getAttributeAsJid("from")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing from attribute"));
        var type = notification.getAttributeAsString("type")
                .orElseThrow(() -> new IllegalArgumentException("notification is missing type attribute"));
        return new SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement(id, from, type);
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
     * Returns the notification sender JID.
     *
     * @return the sender JID; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the notification type, always {@code "psa"} for this RPC.
     *
     * @return the type; never {@code null}
     */
    public String notificationType() {
        return notificationType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds an {@code <ack class="notification">} stanza carrying the
     * {@code id}, {@code to}, and {@code type} attributes lifted from the
     * source notification.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <ack/>} stanza
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPsaResetSmbLastQpPrefetchTimestampResponseAck",
            exports = "makeResetSmbLastQpPrefetchTimestampResponseAck",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("ack")
                .attribute("id", notificationId)
                .attribute("to", notificationFrom)
                .attribute("class", "notification")
                .attribute("type", notificationType);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType);
    }

    @Override
    public String toString() {
        return "SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType + ']';
    }
}
