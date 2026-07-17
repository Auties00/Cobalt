package com.github.auties00.cobalt.wire.stanza.iq.account;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Requests that the relay forget the pairing record for a companion device.
 *
 * <p>This is the outbound {@code <iq xmlns="md" type="set">} stanza that tears down the
 * server-side multi-device record for a single companion. It is sent during logout, before the
 * local socket is closed, and may also be sent to unpair an arbitrary secondary device while the
 * client remains connected. On success the relay echoes the {@code <remove-companion-device>}
 * payload back inside an {@code <iq type="result">} envelope.
 */
@WhatsAppWebModule(moduleName = "WAWebUnpairDeviceJob")
public final class IqUnpairDeviceRequest implements IqStanza.Request {
    /**
     * Holds the companion-device {@link Jid} being unpaired.
     *
     * <p>The value is written verbatim into the {@code <remove-companion-device jid=...>}
     * attribute of {@link #toStanza()}.
     *
     * @implNote
     * This implementation accepts any {@link Jid} so that a primary client may unpair an
     * arbitrary companion; WA Web only ever fills this with the self device for the logout path.
     */
    private final Jid deviceJid;

    /**
     * Holds the free-form, caller-supplied reason string.
     *
     * <p>The value is written verbatim into the {@code <remove-companion-device reason=...>}
     * attribute and surfaced server-side as telemetry; the relay does not validate its contents.
     */
    private final String reason;

    /**
     * Constructs an unpair-device request from the given device handle and reason.
     *
     * <p>Both arguments are stored as-is and later routed verbatim into the outbound stanza by
     * {@link #toStanza()}; no validation beyond the {@code null} checks is performed.
     *
     * @param deviceJid the companion-device {@link Jid} to unpair
     * @param reason    the caller-supplied free-form reason string
     * @throws NullPointerException if either argument is {@code null}
     */
    public IqUnpairDeviceRequest(Jid deviceJid, String reason) {
        this.deviceJid = Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
    }

    /**
     * Returns the companion-device {@link Jid} being unpaired.
     *
     * @return the device {@link Jid}, never {@code null}
     */
    public Jid deviceJid() {
        return deviceJid;
    }

    /**
     * Returns the free-form unpair reason.
     *
     * @return the reason string, never {@code null}
     */
    public String reason() {
        return reason;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces an {@code <iq xmlns="md" type="set">} envelope addressed to
     * {@link Jid#userServer()} and wrapping a single {@code <remove-companion-device>} child that
     * carries the {@code jid} and {@code reason} attributes.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the
     *         {@code <remove-companion-device>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUnpairDeviceJob",
            exports = "unpairDevice", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var removePayload = new StanzaBuilder()
                .description("remove-companion-device")
                .attribute("jid", deviceJid)
                .attribute("reason", reason)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "md")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(removePayload);
    }

    /**
     * Compares this request to another object for value equality.
     *
     * <p>Two requests are equal when they target the same {@link #deviceJid()} and carry the same
     * {@link #reason()}.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an equal {@link IqUnpairDeviceRequest}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqUnpairDeviceRequest) obj;
        return Objects.equals(this.deviceJid, that.deviceJid)
                && Objects.equals(this.reason, that.reason);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code derived from {@link #deviceJid()} and {@link #reason()}
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceJid, reason);
    }

    /**
     * Returns a debug representation of this request.
     *
     * @return a string containing the {@link #deviceJid()} and {@link #reason()}
     */
    @Override
    public String toString() {
        return "IqUnpairDeviceRequest[deviceJid=" + deviceJid
                + ", reason=" + reason + ']';
    }
}
