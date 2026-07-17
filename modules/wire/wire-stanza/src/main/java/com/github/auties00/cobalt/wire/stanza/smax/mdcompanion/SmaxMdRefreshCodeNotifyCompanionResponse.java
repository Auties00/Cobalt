package com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound {@code <notification type="link_code_companion_reg" stage="refresh_code">}
 * stanza pushed to a companion when the primary device rotates the link-code pairing reference.
 *
 * <p>The companion consumes this projection to rerender the eight-character link-code display
 * when the rolling pairing window expires or when the user explicitly requests a new code on the
 * primary. The {@code force_manual_refresh} attribute distinguishes a manual primary trigger
 * from the natural rotation cadence, and the new pairing ref replaces the displayed one only when
 * it matches the ref the companion already holds. An inbound notification is acknowledged with a
 * {@link SmaxMdRefreshCodeNotifyCompanionAcknowledgement} built from this projection.
 *
 * @implNote This implementation mirrors WA Web's validation set: the tag is {@code notification},
 * the {@code type} is {@code link_code_companion_reg}, the {@code from} is the
 * {@code s.whatsapp.net} domain, the nested {@code <link_code_companion_reg/>} child carries
 * {@code stage="refresh_code"}, the optional {@code force_manual_refresh} attribute is
 * constrained to {@code "true"} or {@code "false"}, and the new {@code <link_code_pairing_ref/>}
 * child resolves to non-empty bytes.
 *
 * @deprecated companion-pairing notify handled inline by {@code IqStreamHandler}.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInMdRefreshCodeNotifyCompanionRequest")
@WhatsAppWebModule(moduleName = "WASmaxInMdServerNotificationMixin")
public final class SmaxMdRefreshCodeNotifyCompanionResponse implements SmaxStanza.Response {
    /**
     * Holds the {@code id} attribute of the inbound notification.
     *
     * <p>Echoed back into the {@link SmaxMdRefreshCodeNotifyCompanionAcknowledgement} stanza's
     * {@code id} attribute by
     * {@link SmaxMdRefreshCodeNotifyCompanionAcknowledgement#from(SmaxMdRefreshCodeNotifyCompanionResponse)}.
     */
    private final String notificationId;

    /**
     * Holds the {@code from} JID of the inbound notification, always the {@code s.whatsapp.net}
     * server domain.
     *
     * <p>Echoed back as the ack's {@code to} attribute by
     * {@link SmaxMdRefreshCodeNotifyCompanionAcknowledgement#from(SmaxMdRefreshCodeNotifyCompanionResponse)}.
     */
    private final Jid notificationFrom;

    /**
     * Holds the optional {@code force_manual_refresh} attribute on the nested
     * {@code <link_code_companion_reg/>}, restricted to {@code "true"} or {@code "false"}.
     *
     * <p>The value {@code "true"} signals that the primary device user pressed the regenerate-code
     * button; {@code "false"} or absent denotes the natural rotation cadence. Any other value is
     * rejected during parsing.
     */
    private final String linkCodeCompanionRegForceManualRefresh;

    /**
     * Holds the new pairing-reference bytes carried in {@code <link_code_pairing_ref/>}.
     *
     * <p>Replaces the previously displayed ref on the companion only when it matches the ref the
     * companion already holds; non-matching refs are dropped.
     */
    private final byte[] linkCodePairingRef;

    /**
     * Constructs the typed projection from already-validated component fields.
     *
     * <p>This is the target of {@link #of(Stanza)} after parsing has succeeded. Public visibility
     * is preserved so unit tests can construct fixtures.
     *
     * @param notificationId                         the notification id; never {@code null}
     * @param notificationFrom                       the notification sender JID; never {@code null}
     * @param linkCodeCompanionRegForceManualRefresh the optional {@code force_manual_refresh} flag; may be {@code null}
     * @param linkCodePairingRef                     the pairing-reference bytes; never {@code null}
     * @throws NullPointerException if any required argument is {@code null}
     */
    public SmaxMdRefreshCodeNotifyCompanionResponse(String notificationId,
                   Jid notificationFrom,
                   String linkCodeCompanionRegForceManualRefresh,
                   byte[] linkCodePairingRef) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.linkCodeCompanionRegForceManualRefresh = linkCodeCompanionRegForceManualRefresh;
        this.linkCodePairingRef = Objects.requireNonNull(linkCodePairingRef, "linkCodePairingRef cannot be null");
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
     * Returns the notification sender JID, always the {@code s.whatsapp.net} server domain.
     *
     * <p>The domain is validated by {@link #of(Stanza)} during parsing.
     *
     * @return the JID; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the optional {@code force_manual_refresh} flag.
     *
     * <p>An empty result is equivalent to {@code "false"}.
     *
     * @return an {@link Optional} carrying {@code "true"} or {@code "false"}, or empty when the
     *         relay omitted the attribute
     */
    public Optional<String> linkCodeCompanionRegForceManualRefresh() {
        return Optional.ofNullable(linkCodeCompanionRegForceManualRefresh);
    }

    /**
     * Returns the new pairing-reference bytes.
     *
     * <p>Compare byte for byte against the companion's currently displayed ref before adopting;
     * non-matching refs are dropped.
     *
     * @return the new pairing-reference bytes; never {@code null}
     */
    public byte[] linkCodePairingRef() {
        return linkCodePairingRef;
    }

    /**
     * Parses a {@code <notification type="link_code_companion_reg" stage="refresh_code"/>}
     * stanza into a typed projection.
     *
     * <p>The companion calls this on every inbound notification of that type to decide whether to
     * rerender the displayed code. The result is {@link Optional#empty()} when the stanza shape
     * diverges from the documented schema.
     *
     * @implNote This implementation enforces tag equality on {@code notification}, an attribute
     * literal on {@code type}, a domain-JID literal on {@code from}, a stage literal on the nested
     * {@code <link_code_companion_reg/>}, the optional {@code force_manual_refresh} constrained to
     * {@code "true"} or {@code "false"}, and a non-empty {@code <link_code_pairing_ref/>} content
     * body.
     *
     * @param stanza the inbound notification stanza
     * @return an {@link Optional} carrying the projection, or empty when the stanza shape diverges
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInMdRefreshCodeNotifyCompanionRequest",
            exports = "parseRefreshCodeNotifyCompanionRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxMdRefreshCodeNotifyCompanionResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "link_code_companion_reg")) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !"s.whatsapp.net".equals(from.server().toString())) {
            return Optional.empty();
        }
        var id = stanza.getAttributeAsString("id").orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        var reg = stanza.getChild("link_code_companion_reg").orElse(null);
        if (reg == null || !reg.hasAttribute("stage", "refresh_code")) {
            return Optional.empty();
        }
        var forceManualRefresh = reg.getAttributeAsString("force_manual_refresh").orElse(null);
        if (forceManualRefresh != null && !"true".equals(forceManualRefresh) && !"false".equals(forceManualRefresh)) {
            return Optional.empty();
        }
        var pairingRef = reg.getChild("link_code_pairing_ref")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (pairingRef == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxMdRefreshCodeNotifyCompanionResponse(id, from, forceManualRefresh, pairingRef));
    }

    /**
     * Compares this projection to another object for value equality.
     *
     * <p>Two projections are equal when their notification id, sender JID, force-manual-refresh
     * flag, and pairing-reference bytes all match.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal projection
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxMdRefreshCodeNotifyCompanionResponse) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.linkCodeCompanionRegForceManualRefresh, that.linkCodeCompanionRegForceManualRefresh)
                && Arrays.equals(this.linkCodePairingRef, that.linkCodePairingRef);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The pairing-reference bytes contribute through {@link Arrays#hashCode(byte[])} so equal
     * contents yield equal codes.
     *
     * @return the hash code derived from the id, sender JID, refresh flag, and pairing ref
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(notificationId, notificationFrom, linkCodeCompanionRegForceManualRefresh);
        result = 31 * result + Arrays.hashCode(linkCodePairingRef);
        return result;
    }

    /**
     * Returns a debug string listing the id, sender JID, refresh flag, and pairing ref.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxMdRefreshCodeNotifyCompanionResponse[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", linkCodeCompanionRegForceManualRefresh=" + linkCodeCompanionRegForceManualRefresh
                + ", linkCodePairingRef=" + Arrays.toString(linkCodePairingRef) + ']';
    }
}
