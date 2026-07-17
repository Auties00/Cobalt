package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A self-addressed envelope that mirrors a message sent from another device
 * registered to the same account.
 *
 * <p>Because WhatsApp supports multi-device, every outgoing message must be
 * delivered not only to the real recipient but also to all of the sender's
 * other devices so that the conversation history remains consistent across
 * them. This envelope wraps the original {@link LinkedMessageContainer} and carries
 * the destination {@link Jid} so that the receiving device can attribute the
 * message to the correct chat.
 *
 * <p>An optional perceptual hash (phash) of the media attachment, when
 * present, lets devices deduplicate identical media uploads without
 * re-downloading the payload.
 */
@ProtobufMessage(name = "Message.DeviceSentMessage")
public final class DeviceSentMessage implements Message {
    /**
     * The {@link Jid} of the chat to which the original message was sent.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid destinationJid;

    /**
     * The original message payload as a {@link LinkedMessageContainer}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LinkedMessageContainer messageContainer;

    /**
     * Optional perceptual hash of the message's media attachment, used to
     * deduplicate identical uploads across devices.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String phash;


    /**
     * Constructs a new device-sent envelope.
     *
     * @param destinationJid    the destination chat JID, may be {@code null}
     * @param messageContainer  the original message container, may be {@code null}
     * @param phash             the perceptual hash of attached media, may be {@code null}
     */
    DeviceSentMessage(Jid destinationJid, LinkedMessageContainer messageContainer, String phash) {
        this.destinationJid = destinationJid;
        this.messageContainer = messageContainer;
        this.phash = phash;
    }

    /**
     * Returns the {@link Jid} of the chat to which the original message was sent.
     *
     * @return an {@link Optional} containing the destination JID, or
     *         {@link Optional#empty()} if no destination is set
     */
    public Optional<Jid> destinationJid() {
        return Optional.ofNullable(destinationJid);
    }

    /**
     * Returns the original message payload carried by this envelope.
     *
     * @return an {@link Optional} containing the {@link LinkedMessageContainer}, or
     *         {@link Optional#empty()} if no payload is set
     */
    public Optional<LinkedMessageContainer> message() {
        return Optional.ofNullable(messageContainer);
    }

    /**
     * Returns the perceptual hash of the message's media attachment.
     *
     * @return an {@link Optional} containing the perceptual hash, or
     *         {@link Optional#empty()} if no hash is set
     */
    public Optional<String> phash() {
        return Optional.ofNullable(phash);
    }

    /**
     * Sets the {@link Jid} of the chat to which the original message was sent.
     *
     * @param destinationJid the new destination JID, or {@code null} to clear it
     */
    public void setDestinationJid(Jid destinationJid) {
        this.destinationJid = destinationJid;
    }

    /**
     * Sets the original message payload carried by this envelope.
     *
     * @param messageContainer the new message container, or {@code null} to clear it
     */
    public void setMessage(LinkedMessageContainer messageContainer) {
        this.messageContainer = messageContainer;
    }

    /**
     * Sets the perceptual hash of the message's media attachment.
     *
     * @param phash the new perceptual hash, or {@code null} to clear it
     */
    public void setPhash(String phash) {
        this.phash = phash;
    }
}
