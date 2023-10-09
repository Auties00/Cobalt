package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.BytesHelper;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A container for unique identifiers and metadata linked to a {@link Message} and contained in
 * {@link MessageInfo}.
 */
@ProtobufMessageName("MessageKey")
public final class MessageKey implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private @NonNull Jid chatJid;

    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    private final boolean fromMe;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final @NonNull String id;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private @Nullable Jid senderJid;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MessageKey(@NonNull Jid chatJid, boolean fromMe, @Nullable String id, @Nullable Jid senderJid) {
        this.chatJid = chatJid;
        this.fromMe = fromMe;
        this.id = Objects.requireNonNull(id, MessageKey::randomId);
        this.senderJid = senderJid;
    }

    public MessageKey(@NonNull Jid chatJid, boolean fromMe) {
        this(chatJid, fromMe, null);
    }

    public MessageKey(@NonNull Jid chatJid, boolean fromMe, @Nullable Jid senderJid) {
        this(chatJid, fromMe, randomId(), senderJid);
    }

    /**
     * Generates a random message id
     *
     * @return a non-null String
     */
    public static String randomId() {
        return HexFormat.of()
                .formatHex(BytesHelper.random(8))
                .toUpperCase(Locale.ROOT);
    }

    public @NonNull Jid chatJid() {
        return chatJid;
    }

    public MessageKey setChatJid(Jid chatJid) {
        this.chatJid = chatJid;
        return this;
    }

    public boolean fromMe() {
        return fromMe;
    }

    public @NonNull String id() {
        return id;
    }

    public Optional<Jid> senderJid() {
        return Optional.ofNullable(senderJid);
    }

    public MessageKey setSenderJid(Jid senderJid) {
        this.senderJid = senderJid;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MessageKey other && Objects.equals(id(), other.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatJid, fromMe, id, senderJid);
    }
}
