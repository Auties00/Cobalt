package com.github.auties00.cobalt.wire.stanza.smax.qp;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound projection of a server-pushed quick-promotion (QP) surfaces notification.
 *
 * <p>This response surfaces the relay's PSA-class push that carries the {@code <surfaces/>} subtree
 * of QP promotions and triggers. Consumers parse one with {@link #of(Stanza)}, fan the contained
 * promotions out through their own QP pipeline, and acknowledge receipt by building a
 * {@link SmaxQpQpNotificationAcknowledgement} via
 * {@link SmaxQpQpNotificationAcknowledgement#from(SmaxQpQpNotificationResponse)}; the ack stops the
 * relay from re-delivering the same push on the next reconnect. The notification's {@code id},
 * {@code from}, and {@code type} are retained as typed state so the ack can echo them without a
 * second lookup against the original stanza.
 *
 * @deprecated not wired: Quick Promotions surfaces have no headless consumer.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInQpSurfacesQPNotificationRequest")
@WhatsAppWebModule(moduleName = "WASmaxInQpSurfacesQPSurfacesMixin")
@WhatsAppWebModule(moduleName = "WASmaxInQpSurfacesServerNotificationMixin")
public final class SmaxQpQpNotificationResponse implements SmaxStanza.Response {
    /**
     * Holds the notification id.
     *
     * <p>This value is echoed verbatim into the acknowledgement stanza so the relay can pair the
     * ack with the originating push.
     */
    private final String notificationId;

    /**
     * Holds the notification sender JID.
     *
     * <p>This value becomes the acknowledgement's {@code to} attribute.
     */
    private final Jid notificationFrom;

    /**
     * Holds the notification type.
     *
     * @implNote
     * This implementation always stores the literal {@code "psa"} after a successful parse; the
     * field is preserved as typed state rather than elided so the ack can echo it without a second
     * lookup against the original stanza.
     */
    private final String notificationType;

    /**
     * Holds the raw {@code <surfaces/>} subtree carrying the QP surfaces, promotions, and triggers
     * payload.
     *
     * @implNote
     * This implementation keeps the subtree as a raw {@link Stanza} rather than re-parsing it through
     * a typed surfaces mixin; consumers route it through their own protobuf pipeline. WA Web's
     * {@code WASmaxInQpSurfacesQPSurfacesMixin} performs the equivalent typed parse upstream, but
     * Cobalt has no per-surface typed model and forwards the subtree verbatim.
     */
    private final Stanza surfacesStanza;

    /**
     * Constructs a new inbound projection from the already-validated stanza fields.
     *
     * <p>This constructor is invoked by {@link #of(Stanza)} once the stanza shape has been validated;
     * callers typically do not instantiate it directly.
     *
     * @param notificationId the notification id; never {@code null}
     * @param notificationFrom the notification sender JID; never {@code null}
     * @param notificationType the notification type; never {@code null}
     * @param surfacesStanza the raw {@code <surfaces/>} subtree; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxQpQpNotificationResponse(String notificationId, Jid notificationFrom, String notificationType,
                   Stanza surfacesStanza) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
        this.surfacesStanza = Objects.requireNonNull(surfacesStanza, "surfacesStanza cannot be null");
    }

    /**
     * Returns the notification id.
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
     * Returns the raw {@code <surfaces/>} subtree.
     *
     * <p>Consumers re-parse this stanza through their own QP-surfaces protobuf pipeline; it is not
     * projected into typed state.
     *
     * @return the {@code <surfaces/>} stanza; never {@code null}
     */
    public Stanza surfacesNode() {
        return surfacesStanza;
    }

    /**
     * Parses an {@link SmaxQpQpNotificationResponse} projection from the given
     * {@code <notification/>} stanza.
     *
     * <p>Returns {@link Optional#empty()} when the envelope is not a {@code <notification
     * type="psa">} carrying a non-null {@code id}, a non-null {@code from}, and a
     * {@code <surfaces/>} child; otherwise returns the parsed projection.
     *
     * @implNote
     * This implementation skips the typed surfaces and server-notification mixins WA Web composes
     * inside {@code parseQPNotificationRequest}; both produce data the consumer would simply forward
     * to the QP pipeline, so the subtree is kept as a raw {@link Stanza} and the typed parse is
     * deferred.
     *
     * @param stanza the inbound notification stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when the stanza shape does not
     *         match
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInQpSurfacesQPNotificationRequest",
            exports = "parseQPNotificationRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxQpQpNotificationResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "psa")) {
            return Optional.empty();
        }
        var id = stanza.getAttributeAsString("id").orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null) {
            return Optional.empty();
        }
        var surfaces = stanza.getChild("surfaces").orElse(null);
        if (surfaces == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxQpQpNotificationResponse(id, from, "psa", surfaces));
    }

    /**
     * Returns whether the given object is a {@link SmaxQpQpNotificationResponse} with equal echoed
     * attributes and surfaces subtree.
     *
     * <p>Two projections are equal when their {@code id}, {@code from}, {@code type}, and
     * {@code <surfaces/>} subtree all match.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when every field matches
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxQpQpNotificationResponse) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType)
                && Objects.equals(this.surfacesStanza, that.surfacesStanza);
    }

    /**
     * Returns a hash code derived from every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType, surfacesStanza);
    }

    /**
     * Returns a debug-friendly textual representation of this projection.
     *
     * <p>The {@code <surfaces/>} subtree is intentionally omitted from the output so logs do not
     * blow up on multi-promotion pushes.
     *
     * @return the textual representation
     */
    @Override
    public String toString() {
        return "SmaxQpQpNotificationResponse[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType + ']';
    }
}
