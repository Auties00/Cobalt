package com.github.auties00.cobalt.message.send.id;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Generates the per-message stanza id placed in the {@code id} attribute of every outbound WhatsApp {@code <message>}.
 * <p>
 * The send pipeline calls {@link #generate(MessageIdVersion, Jid)} once per outgoing message; the returned string is the
 * wire identifier the server, the recipient, and every downstream acknowledgement use to refer to the message. Callers
 * that pass Cobalt a pre-built {@link com.github.auties00.cobalt.wire.core.message.MessageKey} have already chosen an id and
 * do not invoke this class. Every id begins with {@value #PREFIX}; the suffix shape depends on the requested
 * {@link MessageIdVersion}.
 *
 * @see MessageIdVersion
 */
@WhatsAppWebModule(moduleName = "WAWebMsgKey")
@WhatsAppWebModule(moduleName = "WAWebMsgKeyNewId")
public final class MessageIdGenerator {
    /**
     * The logger for {@link MessageIdGenerator}.
     */
    private static final System.Logger LOGGER = Log.get(MessageIdGenerator.class);

    /**
     * Holds the four-character prefix shared by every WhatsApp Web message id.
     * <p>
     * The value is fixed on both client and server, so an incoming id can be checked against this prefix as a sanity
     * test.
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgKey", exports = {"newId", "newId_DEPRECATED"},
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String PREFIX = "3EB0";

    /**
     * Holds the number of leading SHA-256 digest bytes used for the V2 hex suffix.
     */
    private static final int V2_DIGEST_SLICE = 9;

    /**
     * Holds the number of random bytes mixed into the V2 pre-image.
     */
    private static final int V2_RANDOM_BYTES = 16;

    /**
     * Holds the number of random bytes hex-encoded into the V1 suffix.
     */
    private static final int V1_RANDOM_BYTES = 8;

    /**
     * Holds the uppercase hex formatter applied to every generated suffix.
     */
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private MessageIdGenerator() {
        throw new AssertionError();
    }

    /**
     * Generates a fresh message id using the supplied {@link MessageIdVersion}.
     * <p>
     * The {@code senderJid} is folded into the V2 pre-image so two accounts sending at the same instant cannot collide
     * deterministically; it is ignored for {@link MessageIdVersion#V1}. Typical usage:
     * {@snippet :
     *     var id = MessageIdGenerator.generate(MessageIdVersion.V2, store.accountStore().jid().orElseThrow());
     * }
     *
     * @implNote This implementation silently falls back to {@link MessageIdVersion#V1} when
     * {@link MessageIdVersion#V2} is requested but the JCA does not provide {@code SHA-256}; the fallback matches WA
     * Web's try/catch-then-{@code newId_DEPRECATED} branch in {@code WAWebMsgKey.newId}.
     * @param version   the algorithm version to use
     * @param senderJid the logged-in user's own PN user {@link Jid}; read only for {@link MessageIdVersion#V2}
     * @return the generated stanza id
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgKey", exports = "newId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static String generate(MessageIdVersion version, Jid senderJid) {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(senderJid, "senderJid");
        return switch (version) {
            case V1 -> generateV1();
            case V2 -> {
                try {
                    yield generateV2(senderJid);
                } catch (NoSuchAlgorithmException e) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "sha-256 unavailable, falling back to v1 message id", e);
                    }
                    yield generateV1();
                }
            }
        };
    }

    /**
     * Generates a {@link MessageIdVersion#V1} message id.
     * <p>
     * The result is the prefix {@value #PREFIX} followed by {@value #V1_RANDOM_BYTES} random bytes formatted as 16
     * uppercase hex characters. This is also the path {@link #generate(MessageIdVersion, Jid)} takes when SHA-256 is
     * unavailable.
     *
     * @return the V1 message id
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgKey", exports = "newId_DEPRECATED",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static String generateV1() {
        var randomBytes = DataUtils.randomByteArray(V1_RANDOM_BYTES);
        return PREFIX + HEX.formatHex(randomBytes);
    }

    /**
     * Generates a {@link MessageIdVersion#V2} message id.
     * <p>
     * The result is the prefix {@value #PREFIX} followed by the first {@value #V2_DIGEST_SLICE} bytes of
     * {@code SHA256(int64(unixTime) || utf8(senderJid) || random(16))}, formatted as 18 uppercase hex characters.
     *
     * @param senderJid the sender PN user {@link Jid} mixed into the pre-image
     * @return the V2 message id
     * @throws NoSuchAlgorithmException if SHA-256 is unavailable
     */
    private static String generateV2(Jid senderJid) throws NoSuchAlgorithmException {
        var payload = genMsgKeyUint(senderJid);
        return getMsgKeyNewSHA256Id(payload);
    }

    /**
     * Builds the SHA-256 pre-image bytes for a {@link MessageIdVersion#V2} message id.
     * <p>
     * The byte layout is {@code int64(unixTime) || utf8(senderJid) || random(16)}. No length prefix precedes the JID;
     * the JID bytes are followed directly by the random bytes, so the pre-image is not parseable by the recipient, only
     * verifiable against the resulting hash.
     *
     * @param senderJid the sender PN user {@link Jid} written verbatim into the pre-image
     * @return the pre-image bytes ready to feed into {@link #getMsgKeyNewSHA256Id(byte[])}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgKeyNewId", exports = "genMsgKeyUint",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static byte[] genMsgKeyUint(Jid senderJid) {
        var timestamp = Instant.now().getEpochSecond();
        var jidBytes = senderJid.toString().getBytes(StandardCharsets.UTF_8);
        var randomBytes = DataUtils.randomByteArray(V2_RANDOM_BYTES);
        var payload = new byte[Long.BYTES + jidBytes.length + randomBytes.length];
        var offset = 0;

        DataUtils.putLong(payload, offset, timestamp, ByteOrder.BIG_ENDIAN);
        offset += Long.BYTES;

        System.arraycopy(jidBytes, 0, payload, offset, jidBytes.length);
        offset += jidBytes.length;

        System.arraycopy(randomBytes, 0, payload, offset, randomBytes.length);

        return payload;
    }

    /**
     * Hashes the pre-image with SHA-256 and returns the prefixed {@link MessageIdVersion#V2} id.
     * <p>
     * The digest is sliced to its first {@value #V2_DIGEST_SLICE} bytes (18 hex characters) so the full id, including
     * the {@value #PREFIX} prefix, fits in 22 characters.
     *
     * @param payload the pre-image bytes produced by {@link #genMsgKeyUint(Jid)}
     * @return {@value #PREFIX} concatenated with 18 uppercase hex characters
     * @throws NoSuchAlgorithmException if SHA-256 is unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgKeyNewId", exports = "getMsgKeyNewSHA256Id",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String getMsgKeyNewSHA256Id(byte[] payload) throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256").digest(payload);
        return PREFIX + HEX.formatHex(digest, 0, V2_DIGEST_SLICE);
    }
}
