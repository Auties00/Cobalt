package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.exception.linked.WhatsAppStreamException;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents one device's slot in an offer's call key fanout.
 *
 * <p>A one to one offer ships the thirty two byte raw end to end call key to each of the peer's
 * devices wrapped in a Signal ciphertext. On the send side each device is one entry inside the
 * offer's {@code <destination>} block, shaped as
 * {@snippet lang="xml" :
 * <to jid="<deviceJid>">
 *   <enc v="2" type="msg|pkmsg" count="0">CIPHERTEXT</enc>
 * </to>
 * }
 * on the server delivered receive side the device's {@code <enc>} sits directly under
 * {@code <offer>} with no {@code <destination>} wrapper. This record models one such device slot in
 * either shape: the addressed device JID and the encrypted key payload, or a bare destination
 * marker carrying no ciphertext.
 *
 * <p>A bare destination (a {@code null} {@link #ciphertext()}) is the all or nothing fallback used
 * when per device encryption fails for any device: every {@code <enc>} is stripped and each device
 * becomes a keyless {@code <to jid="..."/>} so the call still rings. The cipher
 * {@link #version() version} is the Signal ciphertext version (always {@code 2} for the call key
 * fanout); the {@link #type() type} is {@code pkmsg} to bootstrap a new session or {@code msg} to
 * continue a ratchet; the sender key group ciphertext {@code skmsg} is never used for the one to one
 * key fanout.
 *
 * @implNote This implementation reuses the regular message fanout Signal envelope shape
 * ({@code v="2"}, {@code type="msg|pkmsg"}, {@code count="0"}); the ciphertext is the encrypted
 * {@code LinkedMessageContainer} carrying the call key and is produced by the message encryption pipeline,
 * not by this record.
 *
 * @param deviceJid  the device this key slot addresses; never {@code null}
 * @param version    the Signal ciphertext version, or {@code -1} for a bare destination
 * @param type       the Signal ciphertext type ({@code msg} or {@code pkmsg}), or {@code null} for a
 *                   bare destination
 * @param count      the Signal retry count, or {@code -1} for a bare destination
 * @param ciphertext the encrypted call key payload, or {@code null} for a bare destination
 * @see SignalingType#OFFER
 */
public record CallKeyDistribution(Jid deviceJid, int version, String type, int count, byte[] ciphertext) {
    /**
     * The wire element tag for the per device wrapper inside {@code <destination>}.
     */
    static final String TO_ELEMENT = "to";

    /**
     * The wire element tag for the encrypted key payload.
     */
    static final String ENC_ELEMENT = "enc";

    /**
     * The wire attribute naming the addressed device JID on a {@code <to>} element.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The wire attribute naming the Signal ciphertext version on an {@code <enc>} element.
     */
    private static final String VERSION_ATTRIBUTE = "v";

    /**
     * The wire attribute naming the Signal ciphertext type on an {@code <enc>} element.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * The wire attribute naming the Signal retry count on an {@code <enc>} element.
     */
    private static final String COUNT_ATTRIBUTE = "count";

    /**
     * Canonicalizes the record components, defensively copying the ciphertext.
     *
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    public CallKeyDistribution {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        ciphertext = ciphertext == null ? null : ciphertext.clone();
    }

    /**
     * Returns a keyed fanout slot carrying the Signal encrypted call key for one device.
     *
     * @param deviceJid  the addressed device JID
     * @param version    the Signal ciphertext version
     * @param type       the Signal ciphertext type ({@code msg} or {@code pkmsg})
     * @param count      the Signal retry count
     * @param ciphertext the encrypted call key payload
     * @return the keyed fanout slot
     * @throws NullPointerException if {@code deviceJid}, {@code type}, or {@code ciphertext} is
     *                              {@code null}
     */
    public static CallKeyDistribution encrypted(Jid deviceJid, int version, String type, int count, byte[] ciphertext) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        return new CallKeyDistribution(deviceJid, version, type, count, ciphertext);
    }

    /**
     * Returns a bare destination slot carrying no key, addressing a device whose encryption failed.
     *
     * @param deviceJid the addressed device JID
     * @return the bare destination slot
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    public static CallKeyDistribution bare(Jid deviceJid) {
        return new CallKeyDistribution(deviceJid, -1, null, -1, null);
    }

    /**
     * Returns whether this slot carries an encrypted key rather than being a bare destination
     * marker.
     *
     * @return {@code true} when a ciphertext is present
     */
    public boolean isEncrypted() {
        return ciphertext != null;
    }

    /**
     * Returns the encrypted call key payload, if present.
     *
     * <p>The returned array, when present, is a defensive copy.
     *
     * @return an {@link Optional} holding a copy of the ciphertext, or empty for a bare destination
     */
    public Optional<byte[]> ciphertextBytes() {
        return Optional.ofNullable(ciphertext)
                .map(byte[]::clone);
    }

    /**
     * Returns the Signal ciphertext type, if present.
     *
     * @return an {@link Optional} holding the type, or empty for a bare destination
     */
    public Optional<String> typeValue() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the raw ciphertext backing this slot.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the
     * stored array cannot be mutated through the returned reference.
     *
     * @return a copy of the ciphertext, or {@code null} for a bare destination
     */
    @Override
    public byte[] ciphertext() {
        return ciphertext == null ? null : ciphertext.clone();
    }

    /**
     * Builds the {@code <to jid="..."><enc .../></to>} stanza for this fanout slot.
     *
     * <p>A bare destination produces a keyless {@code <to jid="..."/>} stanza with no {@code <enc>}
     * child; a keyed slot wraps the {@code <enc>} key stanza.
     *
     * @return the fanout slot stanza
     */
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description(TO_ELEMENT)
                .attribute(JID_ATTRIBUTE, deviceJid);
        if (ciphertext != null) {
            builder.content(new StanzaBuilder()
                    .description(ENC_ELEMENT)
                    .attribute(VERSION_ATTRIBUTE, version, version >= 0)
                    .attribute(TYPE_ATTRIBUTE, type)
                    .attribute(COUNT_ATTRIBUTE, count, count >= 0)
                    .content(ciphertext)
                    .build());
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <to>} fanout slot into a {@link CallKeyDistribution}.
     *
     * <p>A {@code <to>} without a {@code jid} attribute yields an empty result. A {@code <to>} with no
     * {@code <enc>} child, or an {@code <enc>} with no content, decodes to a {@link #bare(Jid) bare}
     * slot.
     *
     * @param stanza the {@code <to>} stanza
     * @return the decoded fanout slot, or an empty result when the stanza carries no device JID
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<CallKeyDistribution> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var deviceJid = stanza.getAttributeAsJid(JID_ATTRIBUTE);
        if (deviceJid.isEmpty()) {
            return Optional.empty();
        }
        var enc = stanza.getChild(ENC_ELEMENT);
        if (enc.isEmpty()) {
            return Optional.of(bare(deviceJid.get()));
        }
        var ciphertext = enc.get().toContentBytes();
        if (ciphertext.isEmpty()) {
            return Optional.of(bare(deviceJid.get()));
        }
        var version = enc.get().getAttributeAsInt(VERSION_ATTRIBUTE, -1);
        // WA treats the <enc> "type" as mandatory and never defaults it: both the call key path
        // (WAWebVoipValidateAndDecryptEnc) and the message path (WAWebHandleMsgParser) read it via
        // ParsableXmlNode.attrEnumValues("type", CiphertextType.members()), whose attrString throws when the
        // attribute is absent, hard rejecting the stanza before decryption. A ciphertext bearing <enc> with no
        // type is therefore malformed rather than a bare destination.
        var type = enc.get().getAttributeAsString(TYPE_ATTRIBUTE)
                .orElseThrow(() -> new WhatsAppStreamException.MalformedNode(
                        "<enc> carrying a call key ciphertext is missing the required \"type\" attribute"));
        var count = enc.get().getAttributeAsInt(COUNT_ATTRIBUTE, -1);
        return Optional.of(CallKeyDistribution.encrypted(deviceJid.get(), version, type, count, ciphertext.get()));
    }

    /**
     * Compares this slot with another for value equality.
     *
     * <p>Two slots are equal when they address the same device, carry the same Signal version, type,
     * and count, and hold ciphertexts that are equal by content. The ciphertext is compared by array
     * contents rather than by reference.
     *
     * @param obj the object to compare with
     * @return {@code true} when {@code obj} is a {@link CallKeyDistribution} equal by value
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof CallKeyDistribution that
                && this.deviceJid.equals(that.deviceJid)
                && this.version == that.version
                && Objects.equals(this.type, that.type)
                && this.count == that.count
                && Arrays.equals(this.ciphertext, that.ciphertext));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The ciphertext contributes its array content hash so that slots equal by value share a hash
     * code.
     *
     * @return the value based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceJid, version, type, count, Arrays.hashCode(ciphertext));
    }

    /**
     * Returns a diagnostic string describing this slot.
     *
     * <p>The ciphertext is summarized by its length rather than its bytes, and a bare destination
     * reports a length of {@code -1}.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "CallKeyDistribution[deviceJid=" + deviceJid
                + ", version=" + version
                + ", type=" + type
                + ", count=" + count
                + ", ciphertextLen=" + (ciphertext == null ? -1 : ciphertext.length)
                + ']';
    }
}
