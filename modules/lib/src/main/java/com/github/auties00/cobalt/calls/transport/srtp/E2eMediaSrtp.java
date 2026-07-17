package com.github.auties00.cobalt.calls.transport.srtp;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protects and unprotects one to one call media with the end to end SRTP transform that the relay forwards
 * opaquely, keyed by the per participant master derived from the call key.
 *
 * <p>A one to one relay call does not apply hop by hop SRTP to its media: the relay is a blind forwarder, so
 * the media on the wire is end to end SRTP rather than relay hop SRTP. Each direction is keyed by a
 * {@value #MASTER_LENGTH} byte master derived from the 32 byte call key and the sending device's JID: the
 * sender keys its outbound stream from its own device JID, and the receiver keys a peer's inbound stream from
 * that peer's device JID, through {@code CallE2eKeyDerivation.deriveSrtpMaster}. The master is expanded into
 * the RFC 3711 {@code AES-CM} session cipher key, salt, and {@code HMAC-SHA1} authentication key; the payload
 * after the RTP header is encrypted with {@code AES-128} counter mode and a {@value #WARP_MI_TAG_LENGTH} byte
 * WARP message integrity tag is appended.
 *
 * <p>The receiver strips the WARP message integrity tag without verifying it, matching the engine; the
 * rollover counter is tracked per synchronization source and estimated from sequence number wraparound. One
 * context holds one direction's key set: a media session builds one from the local device's master to
 * {@link #protectRtp(byte[], int) protect} outbound packets and one from the peer device's master to
 * {@link #unprotectRtp(byte[], int) unprotect} inbound packets.
 */
public final class E2eMediaSrtp {
    /**
     * The logger for {@link E2eMediaSrtp}.
     */
    private static final System.Logger LOGGER = Log.get(E2eMediaSrtp.class);

    /**
     * The length, in bytes, of the trailing WARP message integrity tag appended to each media packet.
     */
    public static final int WARP_MI_TAG_LENGTH = 4;

    /**
     * The length, in bytes, of the derived SRTP master (a sixteen byte key followed by a fourteen byte salt).
     */
    private static final int MASTER_LENGTH = 30;

    /**
     * The fixed RTP header length, in bytes, before any contributing source identifiers or header extension.
     */
    private static final int RTP_FIXED_HEADER_LENGTH = 12;

    /**
     * The RFC 3711 {@code AES-CM} key derivation label selecting the session cipher key.
     */
    private static final int LABEL_CIPHER = 0x00;

    /**
     * The RFC 3711 {@code AES-CM} key derivation label selecting the session authentication key.
     */
    private static final int LABEL_AUTH = 0x01;

    /**
     * The RFC 3711 {@code AES-CM} key derivation label selecting the session salt.
     */
    private static final int LABEL_SALT = 0x02;

    /**
     * Holds the sixteen byte {@code AES-128} session cipher key.
     */
    private final byte[] cipherKey;

    /**
     * Holds the twenty byte {@code HMAC-SHA1} session authentication key that keys the WARP message integrity
     * tag.
     */
    private final byte[] authKey;

    /**
     * Holds the fourteen byte session salt seeding the per packet counter nonce.
     */
    private final byte[] salt;

    /**
     * Holds the immutable {@code AES-128} session key specification, shared safely across threads.
     */
    private final SecretKeySpec cipherKeySpec;

    /**
     * Holds the immutable {@code HMAC-SHA1} session key specification, shared safely across threads.
     */
    private final SecretKeySpec authKeySpec;

    /**
     * Holds the per thread {@code AES-128} counter mode engine reused across packets.
     *
     * <p>The outbound {@link #protectRtp(byte[], int)} path is driven by both the audio capture pump thread
     * and the video encode thread, and {@link #cryptPayload(byte[], int, int, int, int, int)} is shared with
     * the inbound path, so the mutable JCA engine is thread confined rather than a shared field to avoid a
     * data race.
     */
    private final ThreadLocal<Cipher> ctrCipher;

    /**
     * Holds the per thread {@code HMAC-SHA1} engine reused across packets for the outbound WARP message
     * integrity tag.
     */
    private final ThreadLocal<Mac> warpMac;

    /**
     * Holds the per thread sixteen byte counter nonce scratch buffer reused across packets.
     */
    private final ThreadLocal<byte[]> nonceScratch;

    /**
     * Holds the per synchronization source inbound rollover counter state used to estimate the forty eight
     * bit packet index of an unprotected packet.
     */
    private final Map<Integer, RolloverState> inboundRoc = new ConcurrentHashMap<>();

    /**
     * Holds the per synchronization source outbound rollover counter state used to stamp the packet index of
     * a protected packet.
     */
    private final Map<Integer, RolloverState> outboundRoc = new ConcurrentHashMap<>();

    /**
     * Constructs an end to end SRTP context from a derived per participant SRTP master.
     *
     * <p>Splits the master into its sixteen byte key and fourteen byte salt, expands the three RFC 3711
     * {@code AES-CM} session values (cipher key, authentication key, salt), and prepares the per thread
     * {@code AES-128} counter mode and {@code HMAC-SHA1} engines.
     *
     * @param master the {@value #MASTER_LENGTH} byte SRTP master from
     *               {@code CallE2eKeyDerivation.deriveSrtpMaster}
     * @throws NullPointerException       if {@code master} is {@code null}
     * @throws IllegalArgumentException   if {@code master} is not exactly {@value #MASTER_LENGTH} bytes long
     * @throws WhatsAppCallException.Srtp if the platform cannot run AES or {@code HMAC-SHA1}
     */
    public E2eMediaSrtp(byte[] master) {
        Objects.requireNonNull(master, "master cannot be null");
        if (master.length != MASTER_LENGTH) {
            throw new IllegalArgumentException("master must be " + MASTER_LENGTH + " bytes, got " + master.length);
        }
        var masterKey = Arrays.copyOfRange(master, 0, 16);
        var masterSalt = Arrays.copyOfRange(master, 16, MASTER_LENGTH);
        this.cipherKey = deriveSessionBytes(masterKey, masterSalt, LABEL_CIPHER, 16);
        this.authKey = deriveSessionBytes(masterKey, masterSalt, LABEL_AUTH, 20);
        this.salt = deriveSessionBytes(masterKey, masterSalt, LABEL_SALT, 14);
        this.cipherKeySpec = new SecretKeySpec(cipherKey, "AES");
        this.authKeySpec = new SecretKeySpec(authKey, "HmacSHA1");
        this.ctrCipher = ThreadLocal.withInitial(() -> {
            try {
                return Cipher.getInstance("AES/CTR/NoPadding");
            } catch (Exception exception) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "e2e srtp aes-ctr engine unavailable", exception);
                }
                throw new WhatsAppCallException.Srtp("end-to-end SRTP AES-CTR unavailable", exception);
            }
        });
        this.warpMac = ThreadLocal.withInitial(() -> {
            try {
                return Mac.getInstance("HmacSHA1");
            } catch (Exception exception) {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "e2e srtp warp-mi hmac engine unavailable", exception);
                }
                throw new WhatsAppCallException.Srtp("end-to-end SRTP WARP-MI HMAC unavailable", exception);
            }
        });
        this.nonceScratch = ThreadLocal.withInitial(() -> new byte[16]);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "e2e media srtp context created");
        }
    }

    /**
     * Encrypts an RTP packet in place toward the peer and appends the WARP message integrity tag.
     *
     * <p>The RTP header is left in the clear; the payload after it is encrypted with {@code AES-128} counter
     * mode under the per packet nonce built from this context's salt, the packet's synchronization source,
     * and the outbound rollover counter, and the {@value #WARP_MI_TAG_LENGTH} byte tag is appended. The
     * buffer must have at least {@value #WARP_MI_TAG_LENGTH} trailing bytes free.
     *
     * @param packet the buffer holding the cleartext RTP packet, with trailing room for the tag
     * @param length the length, in bytes, of the cleartext RTP packet
     * @return the length, in bytes, of the protected packet ({@code length} grown by
     *         {@value #WARP_MI_TAG_LENGTH})
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalArgumentException   if {@code length} is not a valid RTP packet within {@code packet}
     * @throws WhatsAppCallException.Srtp if the AES or {@code HMAC-SHA1} computation fails
     */
    public int protectRtp(byte[] packet, int length) {
        Objects.requireNonNull(packet, "packet cannot be null");
        var headerLength = rtpHeaderLength(packet, length);
        var ssrc = readSsrc(packet);
        var sequence = ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        var roc = outboundRoc.computeIfAbsent(ssrc, _ -> new RolloverState()).advance(sequence);
        cryptPayload(packet, headerLength, length - headerLength, ssrc, roc, sequence);
        var tag = warpMiTag(packet, length, roc);
        System.arraycopy(tag, 0, packet, length, WARP_MI_TAG_LENGTH);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "e2e srtp rtp protected, ssrc={0} sequence={1} roc={2}", ssrc, sequence, roc);
        }
        return length + WARP_MI_TAG_LENGTH;
    }

    /**
     * Decrypts an RTP packet in place that arrived from the peer, stripping the WARP message integrity tag.
     *
     * <p>The trailing {@value #WARP_MI_TAG_LENGTH} byte tag is stripped without verification (the engine does
     * not authenticate inbound media at this layer); the payload after the RTP header is then decrypted with
     * {@code AES-128} counter mode under the per packet nonce built from this context's salt, the packet's
     * synchronization source, and the per source rollover counter estimated from the sequence number.
     *
     * @param packet the buffer holding the protected RTP packet
     * @param length the length, in bytes, of the protected RTP packet
     * @return the length, in bytes, of the cleartext RTP packet ({@code length} shrunk by
     *         {@value #WARP_MI_TAG_LENGTH})
     * @throws NullPointerException       if {@code packet} is {@code null}
     * @throws IllegalArgumentException   if {@code length} is not a valid protected RTP packet within
     *                                    {@code packet}
     * @throws WhatsAppCallException.Srtp if the AES computation fails
     */
    public int unprotectRtp(byte[] packet, int length) {
        Objects.requireNonNull(packet, "packet cannot be null");
        if (length < RTP_FIXED_HEADER_LENGTH + WARP_MI_TAG_LENGTH) {
            throw new IllegalArgumentException("protected packet too short: " + length);
        }
        var payloadEnd = length - WARP_MI_TAG_LENGTH;
        var headerLength = rtpHeaderLength(packet, payloadEnd);
        var ssrc = readSsrc(packet);
        var sequence = ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        var roc = inboundRoc.computeIfAbsent(ssrc, _ -> new RolloverState()).advance(sequence);
        cryptPayload(packet, headerLength, payloadEnd - headerLength, ssrc, roc, sequence);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "e2e srtp rtp unprotected, ssrc={0} sequence={1} roc={2}", ssrc, sequence, roc);
        }
        return payloadEnd;
    }

    /**
     * Encrypts or decrypts a packet's payload in place with {@code AES-128} counter mode.
     *
     * <p>Counter mode is symmetric, so the same call both encrypts and decrypts; the per packet counter nonce
     * is built by {@link #buildNonce(int, int, int)} from this context's salt and the packet's
     * synchronization source and index. A payload length of zero or less is a no op.
     *
     * @param packet     the buffer holding the packet
     * @param offset     the offset of the payload within {@code packet}
     * @param payloadLen the length, in bytes, of the payload to transform
     * @param ssrc       the packet's synchronization source
     * @param roc        the packet's rollover counter
     * @param sequence   the packet's sixteen bit RTP sequence number
     * @throws WhatsAppCallException.Srtp if the AES computation fails
     * @implNote This implementation transforms the payload in place through the copy-safe
     * {@link Cipher#doFinal(ByteBuffer, ByteBuffer)} overload, wrapping the payload region in a
     * {@link ByteBuffer} and passing its {@link ByteBuffer#duplicate() duplicate} as the output. That
     * overload is contractually copy-safe (its input and output buffers may reference the same backing
     * array), so no per packet payload sized output array is allocated and no separate copy back is needed;
     * the {@code byte[]} {@code doFinal} overloads carry no such overlap guarantee, which is why the buffer
     * form is used on this per packet path.
     */
    private void cryptPayload(byte[] packet, int offset, int payloadLen, int ssrc, int roc, int sequence) {
        if (payloadLen <= 0) {
            return;
        }
        try {
            var cipher = ctrCipher.get();
            cipher.init(Cipher.ENCRYPT_MODE, cipherKeySpec,
                    new IvParameterSpec(buildNonce(ssrc, roc, sequence)));
            var payload = ByteBuffer.wrap(packet, offset, payloadLen);
            cipher.doFinal(payload, payload.duplicate());
        } catch (Exception exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "e2e srtp aes-ctr failed for ssrc=" + ssrc, exception);
            }
            throw new WhatsAppCallException.Srtp("end-to-end SRTP AES-CTR failed", exception);
        }
    }

    /**
     * Builds the sixteen byte AES counter nonce for a packet.
     *
     * <p>The salt occupies the high fourteen bytes; the synchronization source is XORed into bytes four
     * through seven and the forty eight bit packet index ({@code (roc << 16) | sequence}) into bytes eight
     * through thirteen; the low two bytes are the {@code AES-CM} block counter and start at zero.
     *
     * @param ssrc     the packet's synchronization source
     * @param roc      the packet's rollover counter
     * @param sequence the packet's sixteen bit RTP sequence number
     * @return the sixteen byte counter nonce
     */
    private byte[] buildNonce(int ssrc, int roc, int sequence) {
        var nonce = nonceScratch.get();
        System.arraycopy(salt, 0, nonce, 0, 14);
        nonce[4] ^= (byte) (ssrc >>> 24);
        nonce[5] ^= (byte) (ssrc >>> 16);
        nonce[6] ^= (byte) (ssrc >>> 8);
        nonce[7] ^= (byte) ssrc;
        var packetIndex = ((long) (roc & 0xFFFFFFFFL) << 16) | (sequence & 0xFFFFL);
        nonce[8] ^= (byte) (packetIndex >>> 40);
        nonce[9] ^= (byte) (packetIndex >>> 32);
        nonce[10] ^= (byte) (packetIndex >>> 24);
        nonce[11] ^= (byte) (packetIndex >>> 16);
        nonce[12] ^= (byte) (packetIndex >>> 8);
        nonce[13] ^= (byte) packetIndex;
        return nonce;
    }

    /**
     * Computes the WARP message integrity tag over a packet.
     *
     * <p>The tag is the first {@value #WARP_MI_TAG_LENGTH} bytes of
     * {@code HMAC-SHA1(authKey, packet[0..length] || rocBigEndian)}.
     *
     * @param packet the buffer holding the packet whose first {@code length} bytes are covered
     * @param length the number of leading bytes of {@code packet} the tag covers
     * @param roc    the packet's rollover counter, appended big endian to the authenticated bytes
     * @return the {@value #WARP_MI_TAG_LENGTH} byte WARP message integrity tag
     * @throws WhatsAppCallException.Srtp if the {@code HMAC-SHA1} computation fails
     */
    private byte[] warpMiTag(byte[] packet, int length, int roc) {
        try {
            var mac = warpMac.get();
            mac.init(authKeySpec);
            mac.update(packet, 0, length);
            mac.update(new byte[]{(byte) (roc >>> 24), (byte) (roc >>> 16), (byte) (roc >>> 8), (byte) roc});
            return Arrays.copyOf(mac.doFinal(), WARP_MI_TAG_LENGTH);
        } catch (Exception exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "e2e srtp warp-mi hmac failed", exception);
            }
            throw new WhatsAppCallException.Srtp("end-to-end SRTP WARP-MI HMAC failed", exception);
        }
    }

    /**
     * Returns the RTP header length, in bytes, of a packet, accounting for contributing source identifiers
     * and a one or two byte header extension.
     *
     * @param packet the buffer holding the RTP packet
     * @param length the length, in bytes, of the RTP packet
     * @return the header length, in bytes
     * @throws IllegalArgumentException if the header does not fit within {@code length}
     */
    private static int rtpHeaderLength(byte[] packet, int length) {
        if (length < RTP_FIXED_HEADER_LENGTH) {
            throw new IllegalArgumentException("RTP packet too short: " + length);
        }
        var csrcCount = packet[0] & 0x0F;
        var headerLength = RTP_FIXED_HEADER_LENGTH + csrcCount * 4;
        var extensionPresent = (packet[0] & 0x10) != 0;
        if (extensionPresent) {
            if (length < headerLength + 4) {
                throw new IllegalArgumentException("RTP extension header does not fit: " + length);
            }
            var extensionWords = ((packet[headerLength + 2] & 0xFF) << 8) | (packet[headerLength + 3] & 0xFF);
            headerLength += 4 + extensionWords * 4;
        }
        if (length < headerLength) {
            throw new IllegalArgumentException("RTP header does not fit: " + length);
        }
        return headerLength;
    }

    /**
     * Reads the thirty two bit synchronization source from an RTP packet.
     *
     * @param packet the buffer holding the RTP packet
     * @return the synchronization source
     */
    private static int readSsrc(byte[] packet) {
        return ((packet[8] & 0xFF) << 24) | ((packet[9] & 0xFF) << 16)
                | ((packet[10] & 0xFF) << 8) | (packet[11] & 0xFF);
    }

    /**
     * Expands one RFC 3711 {@code AES-CM} session value from a master key and salt.
     *
     * <p>The counter is the master salt in its high fourteen bytes with the label XORed into byte seven;
     * {@code AES-128} counter mode over zero bytes yields the keystream that is the session value.
     *
     * @param masterKey  the sixteen byte master key
     * @param masterSalt the fourteen byte master salt
     * @param label      the key derivation label
     * @param length     the session value length, in bytes
     * @return the {@code length} byte session value
     * @throws WhatsAppCallException.Srtp if the AES computation fails
     */
    private static byte[] deriveSessionBytes(byte[] masterKey, byte[] masterSalt, int label, int length) {
        var counter = new byte[16];
        System.arraycopy(masterSalt, 0, counter, 0, 14);
        counter[7] ^= (byte) label;
        try {
            var cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(masterKey, "AES"), new IvParameterSpec(counter));
            return cipher.doFinal(new byte[length]);
        } catch (Exception exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "e2e srtp session-key derivation failed", exception);
            }
            throw new WhatsAppCallException.Srtp("end-to-end SRTP session-key derivation failed", exception);
        }
    }

    /**
     * Tracks a rollover counter and the last sequence number to estimate the forty eight bit packet index
     * across the sixteen bit RTP sequence space.
     */
    private static final class RolloverState {
        /**
         * Holds the current rollover counter.
         */
        private int roc;

        /**
         * Holds the last sequence number observed, or {@code -1} before the first packet.
         */
        private int lastSequence = -1;

        /**
         * Advances the rollover counter for a newly observed sequence number and returns the counter to use
         * for that packet.
         *
         * <p>The first packet seeds the state and returns the current counter; a later packet whose sequence
         * number jumped backward by more than half the sequence space is treated as a wraparound and
         * increments the counter.
         *
         * @param sequence the packet's sixteen bit RTP sequence number
         * @return the rollover counter for this packet
         */
        private int advance(int sequence) {
            if (lastSequence < 0) {
                lastSequence = sequence;
                return roc;
            }
            if (sequence < lastSequence && lastSequence - sequence > 0x8000) {
                roc++;
            }
            lastSequence = sequence;
            return roc;
        }
    }
}
