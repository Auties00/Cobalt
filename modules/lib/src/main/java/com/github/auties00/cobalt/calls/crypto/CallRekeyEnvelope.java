package com.github.auties00.cobalt.calls.crypto;

import com.github.auties00.cobalt.calls.signaling.session.CallEncOptions;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds one connected participant device's encrypted slot in a group call {@code <enc_rekey>} fanout.
 *
 * <p>A group rekey reshares a fresh thirty two byte end to end call key to every other connected
 * participant device. Unlike the offer's call key fanout, a rekey is NOT one stanza carrying a
 * per device {@code <destination>} block: each connected participant emits its OWN {@code <enc_rekey>}
 * stanza, addressed unicast to ONE recipient device, carrying exactly ONE {@code <enc>} child (the
 * Signal ciphertext of {@code LinkedMessageContainer{Call{callKey}}}). This record models one such unicast
 * slot: the addressed device, the Signal envelope, and whether the envelope bootstraps a new session
 * (so the sender knows it must attach its {@code <device-identity>}).
 *
 * <p>On the wire one such slot becomes the body of a {@code <call to="<recipientDeviceLid>">} stanza:
 * {@snippet lang="xml" :
 * <enc_rekey call-id="..." call-creator="..." transaction-id="N">
 *   <encopt keygen="2"/>
 *   <enc v="2" type="pkmsg|msg" count="0">CIPHERTEXT</enc>
 *   <device-identity>ADV ~185 bytes</device-identity>  <!-- only when type="pkmsg" -->
 * </enc_rekey>
 * }
 *
 * @implNote This implementation models the unicast rekey slot as a single {@code <enc>} per stanza
 * (NOT a per device fanout), an {@code <encopt keygen="2"/>} sibling matching the offer's keygen
 * version, and a {@code <device-identity>} attached only on a {@code pkmsg} envelope. The plaintext the
 * {@code <enc>} encrypts is the SAME {@code LinkedMessageContainer{Call{callKey}}} structure as the offer key
 * (a single thirty two byte raw key); the three per domain rekey keys (audio, video, appdata) are
 * derived LOCALLY from this one key, not transmitted.
 *
 * @param recipientDevice the participant device this rekey slot is addressed to; never {@code null}
 * @param type            the Signal envelope variant of the {@code <enc>} payload; never {@code null}
 * @param ciphertext      the Signal ciphertext of {@code LinkedMessageContainer{Call{callKey}}}; never
 *                        {@code null}
 * @param deviceIdentity  the ADV device identity bytes to attach when {@code type} is
 *                        {@link MessageEncryptionType#PKMSG}, or {@code null} when none is available
 * @see LiveCallKeyExchange
 */
public record CallRekeyEnvelope(Jid recipientDevice, MessageEncryptionType type, byte[] ciphertext,
                                byte[] deviceIdentity) {
    /**
     * The wire element tag for a rekey action.
     *
     * <p>This is the {@code <enc_rekey>} element name {@link #toNode(String, Jid, int)} stamps, the same
     * literal {@link com.github.auties00.cobalt.calls.signaling.incall.RekeyStanza#ELEMENT} carries on the
     * decode side.
     */
    public static final String ELEMENT = "enc_rekey";

    /**
     * The wire element tag for the Signal encrypted rekey key payload.
     */
    private static final String ENC_ELEMENT = "enc";

    /**
     * The wire element tag for the ADV device identity block.
     */
    private static final String DEVICE_IDENTITY_ELEMENT = "device-identity";

    /**
     * The wire attribute naming the rotation transaction id on an {@code <enc_rekey>} element.
     */
    static final String TRANSACTION_ID_ATTRIBUTE = "transaction-id";

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
     * The retry count value stamped on every call key {@code <enc>} element.
     *
     * <p>Every offer and rekey {@code <enc>} carries {@code count="0"}; the call key fanout never
     * recounts because a failed envelope triggers a fresh offer rather than a retry on the same
     * {@code <enc>}.
     */
    private static final int ENC_COUNT = 0;

    /**
     * Canonicalizes the record components, defensively copying the binary payloads.
     *
     * @throws NullPointerException if {@code recipientDevice}, {@code type}, or {@code ciphertext} is
     *                              {@code null}
     */
    public CallRekeyEnvelope {
        Objects.requireNonNull(recipientDevice, "recipientDevice cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        ciphertext = ciphertext.clone();
        deviceIdentity = deviceIdentity == null ? null : deviceIdentity.clone();
    }

    /**
     * Returns the ADV device identity bytes, if present.
     *
     * <p>The returned array, when present, is a defensive copy. A device identity is attached only to a
     * {@link MessageEncryptionType#PKMSG} envelope; a {@link MessageEncryptionType#MSG} envelope carries
     * none.
     *
     * @return an {@link Optional} holding a copy of the device identity bytes, or empty when absent
     */
    public Optional<byte[]> deviceIdentityBytes() {
        return Optional.ofNullable(deviceIdentity)
                .map(byte[]::clone);
    }

    /**
     * Returns the Signal ciphertext backing this rekey slot.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the Signal ciphertext
     */
    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    /**
     * Returns the ADV device identity bytes backing this rekey slot.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy.
     *
     * @return a copy of the device identity bytes, or {@code null} when absent
     */
    @Override
    public byte[] deviceIdentity() {
        return deviceIdentity == null ? null : deviceIdentity.clone();
    }

    /**
     * Builds the {@code <enc_rekey>} action stanza addressed to this recipient device.
     *
     * <p>The common header is stamped first, then the rotation {@code transaction-id} when present, then
     * the {@code <encopt keygen="2"/>} sibling, the single {@code <enc>} envelope, and finally the
     * {@code <device-identity>} child when this slot bootstraps a new session and a device identity is
     * available. The {@code <enc>} carries the Signal ciphertext version
     * {@value MessageEncryption#CIPHERTEXT_VERSION}, this slot's {@link #type() type}, and the
     * {@value #ENC_COUNT} retry count.
     *
     * @param callId        the call identifier
     * @param callCreator   the call creator's device JID (this sender's own device JID)
     * @param transactionId the rotation transaction id, or {@code -1} to omit it
     * @return the rekey action stanza
     * @throws NullPointerException if {@code callId} or {@code callCreator} is {@code null}
     */
    public Stanza toNode(String callId, Jid callCreator, int transactionId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        var children = new ArrayList<Stanza>(3);
        children.add(CallEncOptions.standard().toStanza());
        children.add(new StanzaBuilder()
                .description(ENC_ELEMENT)
                .attribute(VERSION_ATTRIBUTE, MessageEncryption.CIPHERTEXT_VERSION)
                .attribute(TYPE_ATTRIBUTE, type.protocolValue())
                .attribute(COUNT_ATTRIBUTE, ENC_COUNT)
                .content(ciphertext)
                .build());
        if (type.isPreKeyMessage() && deviceIdentity != null) {
            children.add(new StanzaBuilder()
                    .description(DEVICE_IDENTITY_ELEMENT)
                    .content(deviceIdentity)
                    .build());
        }
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(LiveCallKeyExchange.CALL_ID_ATTRIBUTE, callId)
                .attribute(LiveCallKeyExchange.CALL_CREATOR_ATTRIBUTE, callCreator)
                .attribute(TRANSACTION_ID_ATTRIBUTE, transactionId, transactionId >= 0)
                .content(children)
                .build();
    }

    /**
     * Compares this rekey slot with another for value equality.
     *
     * <p>Two slots are equal when they address the same {@link #recipientDevice() recipient device},
     * carry the same {@link #type() envelope type}, and hold byte identical {@code ciphertext} and
     * {@code deviceIdentity} arrays. This override replaces the record default so the binary payloads are
     * compared by content through {@link Arrays#equals(byte[], byte[])} rather than by reference.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is a {@link CallRekeyEnvelope} with equal components
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof CallRekeyEnvelope that
                && this.recipientDevice.equals(that.recipientDevice)
                && this.type == that.type
                && Arrays.equals(this.ciphertext, that.ciphertext)
                && Arrays.equals(this.deviceIdentity, that.deviceIdentity));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>This override replaces the record default so the {@code ciphertext} and {@code deviceIdentity}
     * arrays contribute their content hashes through {@link Arrays#hashCode(byte[])} rather than their
     * identity hashes.
     *
     * @return the content based hash code of this rekey slot
     */
    @Override
    public int hashCode() {
        return Objects.hash(recipientDevice, type, Arrays.hashCode(ciphertext), Arrays.hashCode(deviceIdentity));
    }

    /**
     * Returns a diagnostic string describing this rekey slot.
     *
     * <p>The binary payloads are rendered as their lengths rather than their contents, with an absent
     * {@code deviceIdentity} shown as a length of {@code -1}, so the ciphertext is never leaked into logs.
     *
     * @return a string carrying the recipient device, envelope type, and payload lengths
     */
    @Override
    public String toString() {
        return "CallRekeyEnvelope[recipientDevice=" + recipientDevice
                + ", type=" + type
                + ", ciphertextLen=" + ciphertext.length
                + ", deviceIdentityLen=" + (deviceIdentity == null ? -1 : deviceIdentity.length)
                + ']';
    }
}
