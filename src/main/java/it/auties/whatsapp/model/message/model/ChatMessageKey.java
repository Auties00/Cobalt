package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Bytes;

import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in
 * {@link ChatMessageInfo}.
 */
@ProtobufMessageName("MessageKey")
public final class ChatMessageKey implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private Jid chatJid;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    private final boolean fromMe;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final String id;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private Jid senderJid;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ChatMessageKey(Jid chatJid, boolean fromMe, String id, Jid senderJid) {
        this.chatJid = chatJid;
        this.fromMe = fromMe;
        this.id = Objects.requireNonNullElseGet(id, ChatMessageKey::randomId);
        this.senderJid = senderJid;
    }

    public ChatMessageKey(Jid chatJid, boolean fromMe) {
        this(chatJid, fromMe, null);
    }

    public ChatMessageKey(Jid chatJid, boolean fromMe, Jid senderJid) {
        this(chatJid, fromMe, randomId(), senderJid);
    }

    /**
     * Generates a random message id
     *
     * @return a non-null String
     */
    public static String randomId() {
        return HexFormat.of()
                .formatHex(Bytes.random(8))
                .toUpperCase(Locale.ROOT);
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
    public boolean equals(Object obj) {
        return obj instanceof ChatMessageKey other && Objects.equals(id(), other.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatJid, fromMe, id, senderJid);
    }
}
