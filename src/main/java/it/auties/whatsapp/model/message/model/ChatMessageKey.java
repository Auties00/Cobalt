package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Bytes;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

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
        this.id = Objects.requireNonNullElse(id, randomIdV2(senderJid));
        this.senderJid = senderJid;
    }

    public ChatMessageKey(Jid chatJid, boolean fromMe) {
        this(chatJid, fromMe, null);
    }

    public ChatMessageKey(Jid chatJid, boolean fromMe, Jid senderJid) {
        this(chatJid, fromMe, randomIdV2(senderJid), senderJid);
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

    /**
     * Generates a random message id based on the Client Type. Generation methods are taken from WEB and Android code
     * @param jid senderJid
     * @param clientType clientType (Mobile or Web)
     * @return a non-null String
     */

    public static String randomIdV2(Jid jid, ClientType... clientType) {
        var type = Objects.requireNonNullElse(clientType[0], ClientType.WEB);
        return switch (type) {
            case ClientType.WEB -> randomWebKeyId(jid);
            case ClientType.MOBILE -> randomMobileKeyId(jid);
        };
    }

    private static String randomWebKeyId(Jid jid) {
        try {
            var random = new Random();
            var meUser = "%s@%s".formatted(jid.user(), "@c.us");
            long timeSeconds = Instant.now().getEpochSecond();
            byte[] randomBytes = new byte[16];
            random.nextBytes(randomBytes);
            var buffer = ByteBuffer.allocate(Long.BYTES + meUser.length() + randomBytes.length);
            buffer.putLong(timeSeconds);
            buffer.put(meUser.getBytes());
            buffer.put(randomBytes);
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(buffer.array());
            byte[] truncatedHash = new byte[9];
            System.arraycopy(hash, 0, truncatedHash, 0, 9);
            return "3EB0" + HexFormat.of().formatHex(truncatedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String randomMobileKeyId(Jid jid) {
        try {
            var random = new Random();
            var messageDigest = MessageDigest.getInstance("MD5");
            var meUser = jid.toSimpleJid().toString().getBytes();
            long timeMillis = System.currentTimeMillis();
            byte[] bArr = new byte[8];
            for (int i = 7; i >= 0; i--) {
                bArr[i] = (byte) timeMillis;
                timeMillis >>= 8;
            }
            messageDigest.update(bArr);
            messageDigest.update(meUser);
            byte[] bArr2 = new byte[16];
            random.nextBytes(bArr2);
            messageDigest.update(bArr2);
            var digested = messageDigest.digest();
            char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            char[] cArr2 = new char[digested.length * 2];
            int i = 0;
            for (byte b : digested) {
                int i2 = b & 255;
                int i3 = i + 1;
                cArr2[i] = cArr[i2 >>> 4];
                i = i3 + 1;
                cArr2[i3] = cArr[i2 & 15];
            }
            return new String(cArr2);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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
