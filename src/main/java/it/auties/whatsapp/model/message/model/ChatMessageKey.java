package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Bytes;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in
 * {@link ChatMessageInfo}.
 */
@ProtobufMessage(name = "MessageKey")
public final class ChatMessageKey {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid chatJid;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean fromMe;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    Jid senderJid;

    public ChatMessageKey(Jid chatJid, boolean fromMe, String id, Jid senderJid) {
        this.chatJid = chatJid;
        this.fromMe = fromMe;
        this.id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        this.senderJid = senderJid;
    }

    /**
     * Generates a random message id
     *
     * @return a non-null String
     */
    public static String randomId(ClientType clientType) {
        return switch (clientType) {
            case ClientType.WEB -> "3EB0" + Bytes.randomHex(13);
            case ClientType.MOBILE -> Bytes.randomHex(16);
        };
    }
    
    public Jid chatJid() {
        return chatJid;
    }

    public ChatMessageKey setChatJid(Jid chatJid) {
        this.chatJid = chatJid;
        return this;
    }

    public boolean fromMe() {
        return fromMe;
    }

    public String id() {
        return id;
    }

    public Optional<Jid> senderJid() {
        return Optional.ofNullable(senderJid);
    }

    public ChatMessageKey setSenderJid(Jid senderJid) {
        this.senderJid = senderJid;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChatMessageKey that && fromMe == that.fromMe && Objects.equals(chatJid, that.chatJid) && Objects.equals(id, that.id) && Objects.equals(senderJid, that.senderJid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatJid, fromMe, id, senderJid);
    }
}
