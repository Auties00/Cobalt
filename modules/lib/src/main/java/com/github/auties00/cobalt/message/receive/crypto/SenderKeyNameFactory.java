package com.github.auties00.cobalt.message.receive.crypto;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;

import java.lang.System.Logger.Level;

/**
 * Builds the {@link SignalSenderKeyName} composite key used by the Signal group cipher
 * to address a per-sender symmetric key inside a group, broadcast, or community chat.
 *
 * <p>{@link #create(Jid, Jid)} is invoked on every send and receive of a sender-key
 * payload and every sender-key distribution import so the libsignal store sees the same
 * composite key regardless of which side built it.
 *
 * @implNote
 * This implementation mirrors WhatsApp Web's
 * {@code WAWebSignalCommonUtils.createSignalLikeSenderKeyName}, which combines the group
 * JID string with a Signal-style {@code user.device} address and feeds the pair to the
 * libsignal sender-key store.
 */
@WhatsAppWebModule(moduleName = "WAWebSignalCommonUtils")
public final class SenderKeyNameFactory {
    /**
     * The logger for {@link SenderKeyNameFactory}.
     */
    private static final System.Logger LOGGER = Log.get(SenderKeyNameFactory.class);

    /**
     * Prevents instantiation of this utility class.
     *
     * <p>The class exposes only the static {@link #create(Jid, Jid)} factory; the
     * constructor throws so reflective instantiation also fails.
     *
     * @throws AssertionError always
     */
    private SenderKeyNameFactory() {
        throw new AssertionError();
    }

    /**
     * Returns the {@link SignalSenderKeyName} that identifies the given sender's
     * sender-key inside the given group.
     *
     * <p>Used by both the send path (sender-key distribution and group encryption) and
     * the receive path (group decryption and sender-key import) so the libsignal store
     * sees the same composite key from either direction.
     *
     * @implNote
     * This implementation passes {@link Jid#device()} directly into the libsignal
     * address; callers that pass a companion JID retain the actual device id rather than
     * the literal {@code "0"} zero-suffix appended by WhatsApp Web's
     * {@code createSignalLikeAddress}.
     *
     * @param groupJid  the group, broadcast, or community JID hosting the sender key
     * @param senderJid the sender's device-level JID
     * @return the sender-key name keyed by the group JID string and the sender's Signal
     *         protocol address
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalCommonUtils", exports = "createSignalLikeSenderKeyName",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static SignalSenderKeyName create(Jid groupJid, Jid senderJid) {
        var senderAddress = new SignalProtocolAddress(senderJid.user(), senderJid.device());
        if (Log.TRACE) LOGGER.log(Level.TRACE, "building sender key name for group {0} sender {1}", groupJid, senderJid);
        return new SignalSenderKeyName(groupJid.toString(), senderAddress);
    }
}
