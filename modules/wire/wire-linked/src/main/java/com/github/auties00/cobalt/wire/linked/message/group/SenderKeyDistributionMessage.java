package com.github.auties00.cobalt.wire.linked.message.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A control message that distributes the Signal sender key for a group
 * to one of its members.
 *
 * <p>WhatsApp group messaging is built on the Signal protocol, which uses
 * per-group symmetric sender keys to efficiently encrypt a single payload
 * that all members can decrypt. Before any encrypted group message can be
 * exchanged, the sender must hand out their sender key to every recipient
 * device using a pairwise Signal session. A {@code SenderKeyDistributionMessage}
 * is the envelope Cobalt uses to carry that key material in a protobuf
 * {@link Message} slot.
 *
 * <p>The payload stored in {@link #axolotlSenderKeyDistributionMessage} is
 * the raw serialized Signal sender key distribution blob produced by the
 * underlying libsignal implementation; Cobalt forwards it opaquely and only
 * pairs it with the {@link Jid group JID} the key is scoped to. Recipients
 * install the key into their local Signal store so they can decrypt subsequent
 * group messages from the sender.
 */
@ProtobufMessage(name = "Message.SenderKeyDistributionMessage")
public final class SenderKeyDistributionMessage implements Message {
    /**
     * The JID of the group to which the distributed sender key applies.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid groupJid;

    /**
     * The serialized Signal sender key distribution payload, as produced by
     * the Signal (Axolotl) library.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] axolotlSenderKeyDistributionMessage;


    /**
     * Constructs a new sender key distribution message.
     *
     * <p>This constructor is package-private because instances are normally
     * created through the generated {@code SenderKeyDistributionMessageBuilder}
     * or by the protobuf deserializer; callers should not invoke it directly.
     *
     * @param groupJid the JID of the group the key is scoped to, or {@code null} if unknown
     * @param axolotlSenderKeyDistributionMessage the serialized Signal sender key bytes, or {@code null}
     */
    SenderKeyDistributionMessage(Jid groupJid, byte[] axolotlSenderKeyDistributionMessage) {
        this.groupJid = groupJid;
        this.axolotlSenderKeyDistributionMessage = axolotlSenderKeyDistributionMessage;
    }

    /**
     * Returns the JID of the group the distributed sender key is scoped to.
     *
     * @return an {@link Optional} containing the group JID, or empty if not set
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns the raw Signal sender key distribution payload.
     *
     * <p>The returned bytes are the opaque output of the Signal library and
     * are meant to be fed back into it to install the key in the recipient's
     * Signal store.
     *
     * @return an {@link Optional} containing the serialized sender key bytes, or empty if not set
     */
    public Optional<byte[]> axolotlSenderKeyDistributionMessage() {
        return Optional.ofNullable(axolotlSenderKeyDistributionMessage);
    }

    /**
     * Sets the JID of the group the distributed sender key applies to.
     *
     * @param groupJid the new group JID, or {@code null} to clear it
     */
    public void setGroupJid(Jid groupJid) {
        this.groupJid = groupJid;
    }

    /**
     * Sets the raw Signal sender key distribution payload.
     *
     * @param axolotlSenderKeyDistributionMessage the new serialized sender key bytes, or {@code null} to clear them
     */
    public void setAxolotlSenderKeyDistributionMessage(byte[] axolotlSenderKeyDistributionMessage) {
        this.axolotlSenderKeyDistributionMessage = axolotlSenderKeyDistributionMessage;
    }
}
