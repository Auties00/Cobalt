package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.crypto.MD5;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.util.Bytes;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HexFormat;
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
        this.id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        this.senderJid = senderJid;
    }

    /**
     * Generates a random message id
     *
     * @return a non-null String
     */
    public static String randomIdV2(Jid jid, ClientType clientType) {
        return switch (clientType) {
            case ClientType.WEB -> {
                var meUser = "%s@%s".formatted(jid.user(), "@c.us");
                var timeSeconds = Instant.now().getEpochSecond();
                var randomBytes = Bytes.random(16);
                var buffer = ByteBuffer.allocate(Long.BYTES + meUser.length() + randomBytes.length);
                buffer.putLong(timeSeconds);
                buffer.put(meUser.getBytes());
                buffer.put(randomBytes);
                yield "3EB0" + HexFormat.of().formatHex(Sha256.calculate(buffer.array()), 0, 9).toUpperCase();
            }
            case ClientType.MOBILE -> {
                var meJid = Objects.requireNonNullElse(jid, Jid.of(JidServer.whatsapp()));
                var meUser = meJid.toSimpleJid().toString().getBytes();
                var timeMillis = System.currentTimeMillis();
                var timeArray = new byte[8];
                for (int i = 7; i >= 0; i--) {
                    timeArray[i] = (byte) timeMillis;
                    timeMillis >>= 8;
                }
                var digested = MD5.calculate(Bytes.concat(timeArray, meUser, Bytes.random(16)));
                var cArr = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
                var cArr2 = new char[digested.length * 2];
                var i = 0;
                for (byte b : digested) {
                    int i2 = b & 255;
                    int i3 = i + 1;
                    cArr2[i] = cArr[i2 >>> 4];
                    i = i3 + 1;
                    cArr2[i3] = cArr[i2 & 15];
                }
                yield new String(cArr2);
            }
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
