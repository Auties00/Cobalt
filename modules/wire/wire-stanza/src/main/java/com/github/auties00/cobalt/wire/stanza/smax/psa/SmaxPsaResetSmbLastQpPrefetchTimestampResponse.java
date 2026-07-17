package com.github.auties00.cobalt.wire.stanza.smax.psa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound projection of the
 * {@code <notification type="psa"><reset_smb_last_qp_prefetch_timestamp/></notification>}
 * stanza that asks the SMB client to refresh its locally-cached
 * quick-promotion prefetch timestamp.
 *
 * <p>A handler reacts to this server-pushed notification by acknowledging it
 * with a {@link SmaxPsaResetSmbLastQpPrefetchTimestampAcknowledgement} and,
 * for SMB accounts, refreshing the quick-promotion data. The projection
 * carries the echoed {@code id}, {@code from}, and {@code type} attributes
 * the ack must echo back, plus the relay-side timestamp and an optional
 * offline-queue hint.
 *
 * @deprecated not wired: SMB QP-prefetch reset has no headless consumer.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WASmaxInPsaResetSmbLastQpPrefetchTimestampRequest")
@WhatsAppWebModule(moduleName = "WASmaxInPsaServerNotificationMixin")
public final class SmaxPsaResetSmbLastQpPrefetchTimestampResponse implements SmaxStanza.Response {
    /**
     * Holds the notification id, echoed verbatim into the ack stanza.
     */
    private final String notificationId;

    /**
     * Holds the notification sender JID, always a user JID since the parser
     * requires the {@code from} attribute to resolve to one.
     */
    private final Jid notificationFrom;

    /**
     * Holds the notification type, always the literal {@code "psa"} since the
     * parser admits only that value.
     */
    private final String notificationType;

    /**
     * Holds the relay-side timestamp carried by the {@code t} attribute as
     * seconds since the Unix epoch.
     */
    private final long timestampSeconds;

    /**
     * Holds the optional {@code offline} hint, the count of offline
     * notifications still queued for delivery, or {@code null} when the
     * attribute was absent.
     */
    private final Integer offline;

    /**
     * Constructs an inbound projection around the parsed notification fields.
     *
     * @param notificationId   the notification id; never {@code null}
     * @param notificationFrom the notification sender JID; never {@code null}
     * @param notificationType the notification type, always {@code "psa"}; never {@code null}
     * @param timestampSeconds the relay-side timestamp in seconds
     * @param offline          the optional offline-queue depth; may be {@code null}
     * @throws NullPointerException if any required argument is {@code null}
     */
    public SmaxPsaResetSmbLastQpPrefetchTimestampResponse(String notificationId, Jid notificationFrom, String notificationType,
                   long timestampSeconds, Integer offline) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType cannot be null");
        this.timestampSeconds = timestampSeconds;
        this.offline = offline;
    }

    /**
     * Returns the notification id.
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
     * Returns the relay-side timestamp in seconds since the Unix epoch.
     *
     * @return the timestamp
     */
    public long timestampSeconds() {
        return timestampSeconds;
    }

    /**
     * Returns the optional offline-queue depth.
     *
     * @return an {@link Optional} carrying the depth, or empty when the
     *         attribute was absent
     */
    public Optional<Integer> offline() {
        return Optional.ofNullable(offline);
    }

    /**
     * Parses a {@code <notification>} stanza into an inbound projection.
     *
     * <p>Requires the {@code <notification>} tag, the literal
     * {@code type="psa"} marker, the
     * {@code <reset_smb_last_qp_prefetch_timestamp/>} child, a JID
     * {@code from} attribute, a {@code t} timestamp attribute, and an
     * {@code id} attribute; any missing element yields
     * {@link Optional#empty()}. The {@code offline} attribute is optional and
     * is treated as absent when missing or negative, surfaced through
     * {@link #offline()}.
     *
     * @implNote
     * This implementation returns an empty {@link Optional} on schema
     * mismatch rather than throwing, diverging from the WA Web parser that
     * raises a {@code SmaxParsingFailure}. A missing {@code offline} attribute
     * is read as the sentinel {@code -1} and then normalised to {@code null}.
     *
     * @param stanza the inbound notification stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty on schema mismatch
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInPsaResetSmbLastQpPrefetchTimestampRequest",
            exports = "parseResetSmbLastQpPrefetchTimestampRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxPsaResetSmbLastQpPrefetchTimestampResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "psa")) {
            return Optional.empty();
        }
        var resetChild = stanza.getChild("reset_smb_last_qp_prefetch_timestamp").orElse(null);
        if (resetChild == null) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null) {
            return Optional.empty();
        }
        var type = stanza.getAttributeAsString("type").orElse(null);
        if (type == null) {
            return Optional.empty();
        }
        var timestampOpt = stanza.getAttributeAsLong("t");
        if (timestampOpt.isEmpty()) {
            return Optional.empty();
        }
        var id = stanza.getAttributeAsString("id").orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        var offlineAttr = stanza.getAttributeAsInt("offline").orElse(-1);
        var offline = offlineAttr < 0 ? null : Integer.valueOf(offlineAttr);
        return Optional.of(new SmaxPsaResetSmbLastQpPrefetchTimestampResponse(id, from, type, timestampOpt.getAsLong(), offline));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPsaResetSmbLastQpPrefetchTimestampResponse) obj;
        return this.timestampSeconds == that.timestampSeconds
                && Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.notificationType, that.notificationType)
                && Objects.equals(this.offline, that.offline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, notificationType, timestampSeconds, offline);
    }

    @Override
    public String toString() {
        return "SmaxPsaResetSmbLastQpPrefetchTimestampResponse[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", notificationType=" + notificationType
                + ", timestampSeconds=" + timestampSeconds
                + ", offline=" + offline + ']';
    }
}
