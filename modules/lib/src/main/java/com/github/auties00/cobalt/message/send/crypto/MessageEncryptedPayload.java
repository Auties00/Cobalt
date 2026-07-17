package com.github.auties00.cobalt.message.send.crypto;

import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * Holds the result of encrypting a single outbound message for one recipient device or group.
 * <p>
 * Produced by {@link MessageEncryption#encryptForDevice(Jid, byte[])} and
 * {@link MessageEncryption#encryptForGroup(Jid, Jid, byte[])}, then consumed by the fanout-stanza builders that write one
 * {@code <enc>} child per recipient on the outgoing {@code <message>}. The {@link #recipientJid} is {@code null} for SKMSG
 * payloads because a sender-key ciphertext is broadcast once to the whole group rather than addressed per device.
 * <p>
 * A {@link #bareDestination(Jid)} marker is a degenerate variant with a {@code null} {@link #type} and {@link #ciphertext}
 * and a non-{@code null} {@link #recipientJid}: it addresses a device without delivering a key, which the call-offer fanout
 * uses to ring every device after stripping all {@code <enc>} on an encryption failure.
 *
 * @param type         the Signal envelope type, one of the constants of {@link MessageEncryptionType}, or {@code null} for a
 *                     bare destination marker
 * @param ciphertext   the encrypted envelope bytes, or {@code null} for a bare destination marker
 * @param recipientJid the recipient device {@link Jid}, or {@code null} for SKMSG group payloads
 */
@WhatsAppWebModule(moduleName = "WAWebEncryptMsgProtobuf")
public record MessageEncryptedPayload(
        MessageEncryptionType type,
        byte[] ciphertext,
        Jid recipientJid
) {
    /**
     * Returns a keyless destination marker addressing a single device with no ciphertext.
     * <p>
     * Used by the call-offer fanout when per-device encryption fails for at least one device: WhatsApp Web then strips every
     * {@code <enc>} and addresses each device with a bare {@code <to jid=.../>}, so the call still rings even though the key
     * is not delivered in the offer. The returned payload has a {@code null} {@link #type()} and {@link #ciphertext()} and
     * must not be treated as an encrypted envelope.
     *
     * @param recipientJid the device the bare destination addresses; never {@code null}
     * @return a keyless {@link MessageEncryptedPayload} marker
     * @throws NullPointerException if {@code recipientJid} is {@code null}
     */
    public static MessageEncryptedPayload bareDestination(Jid recipientJid) {
        Objects.requireNonNull(recipientJid, "recipientJid cannot be null");
        return new MessageEncryptedPayload(null, null, recipientJid);
    }

    /**
     * Returns whether this payload establishes a new Signal session.
     * <p>
     * A {@code true} return means the recipient does not yet hold the sender's identity, so the fanout-stanza writer must
     * accompany the {@code <enc>} element with an {@code <identity>} sibling.
     *
     * @return {@code true} when {@link #type} is {@link MessageEncryptionType#PKMSG}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateFanoutStanza", exports = "createFanoutMsgStanza",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean isPreKeyMessage() {
        return type.isPreKeyMessage();
    }

    /**
     * Returns whether this payload is a sender-key group message.
     * <p>
     * Distinguishes SKMSG payloads, delivered via a shared sender key, from the per-device PKMSG and MSG payloads, so the
     * stanza writer knows whether to emit a single broadcast {@code <enc>} or one element per recipient device.
     *
     * @return {@code true} when {@link #type} is {@link MessageEncryptionType#SKMSG}
     */
    public boolean isSenderKeyMessage() {
        return type.isSenderKeyMessage();
    }
}
