package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.ServerMessage;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a message that refers to a message sent by the device paired with
 * the active WhatsappWeb session.
 */
@ProtobufMessage(name = "Message.DeviceSentMessage")
public final class DeviceSentMessage implements ServerMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid destinationJid;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final MessageContainer message;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String phash;

    DeviceSentMessage(Jid destinationJid, MessageContainer message, String phash) {
        this.destinationJid = Objects.requireNonNull(destinationJid, "destinationJid cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.phash = phash;
    }

    public Jid destinationJid() {
        return destinationJid;
    }

    public MessageContainer message() {
        return message;
    }

    public Optional<String> phash() {
        return Optional.ofNullable(phash);
    }

    @Override
    public Type type() {
        return Type.DEVICE_SENT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeviceSentMessage that
                && Objects.equals(destinationJid, that.destinationJid)
                && Objects.equals(message, that.message)
                && Objects.equals(phash, that.phash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationJid, message, phash);
    }

    @Override
    public String toString() {
        return "DeviceSentMessage[" +
                "destinationJid=" + destinationJid +
                ", message=" + message +
                ", phash=" + phash +
                ']';
    }
}