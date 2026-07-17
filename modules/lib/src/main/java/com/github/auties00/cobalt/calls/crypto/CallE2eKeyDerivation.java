package com.github.auties00.cobalt.calls.crypto;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RekeyKeyType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Derives the end to end call key chain that protects a call's media: from the 32-byte raw end to end
 * key down to the per participant SFrame base key, the per participant SRTP master, and the hop by hop
 * SRTP key set.
 *
 * <p>A call has a single 32-byte raw end to end key (the {@code callKey}), minted by the caller and
 * fanned out per device inside the offer's Signal encrypted {@code <enc>} envelope, then exchanged again
 * on a group rekey. That one secret never directly encrypts media; instead every endpoint expands it,
 * deterministically and identically, into the actual media keys through this chain:
 *
 * <ul>
 *   <li><b>SFrame base key</b> (per participant device): the inner end to end key. The audio and video
 *       payloads are SFrame encrypted by the sender, the relay or SFU forwards opaque ciphertext, and
 *       the receiver decrypts. Because the participant device JID is mixed into the derivation, every
 *       participant device gets a distinct base key from the same {@code callKey}; this is the
 *       group/SFU media crypto key (1:1 calls use shared key SRTP and do not exercise SFrame).</li>
 *   <li><b>SRTP master</b> (per participant device): the per direction transport key. A sender keys its
 *       outbound stream from its own device JID and its inbound stream from the peer's device JID. On a
 *       1:1 call these feed the transport peer to peer SRTP slots directly.</li>
 *   <li><b>Hop by hop SRTP key set</b>: derived not from the {@code callKey} but from the 30-byte
 *       relay {@code <hbh_key>} the relay hands out in the call ack, this protects only the
 *       client to relay leg so the relay can route the (still SFrame opaque) media RTP. See
 *       {@link #deriveHbhSrtpMaster(byte[], HopByHopGroup)}.</li>
 * </ul>
 *
 * <p>The chain is gated on the key generation version: only {@value #SUPPORTED_KEYGEN_VER} is accepted,
 * matching the engine which rejects any other with an unsupported keygen version error. The three
 * {@link RekeyKeyType} domains (audio, video, AppData) are a <i>local</i> split: the wire carries a
 * single 32-byte key and each endpoint derives the per domain material on its own, so no per domain
 * key is ever transmitted.
 *
 * <p>All derivations are RFC 5869 HKDF SHA256 computed through {@link VoipCryptoNative}; entropy for
 * minting the raw key is {@link VoipCryptoNative#randomBytes(int)}. Every method is stateless and
 * thread safe.
 *
 * @implNote This implementation supports only key generation version {@value #SUPPORTED_KEYGEN_VER},
 *           whose SFrame base key is domain agnostic: one base key per participant device, shared across
 *           the audio, video, and AppData {@link RekeyKeyType} domains. The per domain SFrame label
 *           variant of later key generation versions is not derived.
 */
public final class CallE2eKeyDerivation {
    /**
     * The logger for {@link CallE2eKeyDerivation}.
     */
    private static final System.Logger LOGGER = Log.get(CallE2eKeyDerivation.class);

    /**
     * Holds the required length, in bytes, of the raw end to end call key.
     *
     * <p>The caller mints exactly this many random bytes; the engine validates the inbound key length
     * is in {@code [1, 32]} and rejects anything longer, so a full length key is always exactly this.
     */
    public static final int RAW_E2E_KEY_LENGTH = 32;

    /**
     * Holds the only supported key generation version.
     *
     * <p>Every participant's crypto block records the key generation version that produced its key
     * material; the engine derives keys only for this version and rejects any other with an
     * unsupported keygen version error.
     */
    public static final int SUPPORTED_KEYGEN_VER = 2;

    /**
     * Holds the length, in bytes, of a derived SFrame base (chain) key.
     *
     * <p>The SFrame key provider receives a key of this length as its chain key and expands per frame
     * keys, salts, and nonces from it.
     */
    public static final int SFRAME_BASE_KEY_LENGTH = 32;

    /**
     * Holds the length, in bytes, of a per participant SRTP master (a 16-byte key followed by a 14-byte
     * salt).
     *
     * <p>This is the {@code AES_CM_128_HMAC_SHA1} master that, split by {@link #srtpMasterKey(byte[])}
     * and {@link #srtpMasterSalt(byte[])}, feeds the RFC 3711 SRTP session key derivation. The leading
     * {@value #SRTP_DIRECTION_KEY_LENGTH}-byte key is the portion the 1:1 transport copies into its
     * peer to peer key slots.
     */
    public static final int SRTP_MASTER_LENGTH = 30;

    /**
     * Holds the length, in bytes, of the AES-128 key portion of an SRTP master.
     *
     * <p>The engine's 1:1 transport copies exactly this many bytes per direction into its peer to peer
     * key slots.
     */
    public static final int SRTP_DIRECTION_KEY_LENGTH = 16;

    /**
     * Holds the length, in bytes, of the relay hop by hop key from the {@code <hbh_key>} element.
     *
     * <p>The relay hands out a base64 {@code <hbh_key>} of this many decoded bytes in the call ack; it
     * keys the client to relay leg, distinct from the {@code callKey} that keys the end to end layer.
     */
    public static final int HBH_KEY_LENGTH = 30;

    /**
     * Holds the length, in bytes, of a derived hop by hop SRTP master (a 16-byte key followed by a
     * 14-byte salt).
     */
    public static final int HBH_SRTP_MASTER_LENGTH = 30;

    /**
     * Holds the length, in bytes, of the derived hop by hop WARP authentication key.
     *
     * <p>Unlike the SRTP and SRTCP masters, which are a 16-byte key plus a 14-byte salt, the WARP
     * authentication key is a raw HMAC key with no salt portion, so its key step output is the full
     * 32 bytes rather than 30.
     */
    public static final int WARP_AUTH_KEY_LENGTH = 32;

    /**
     * Holds the length, in bytes, of each half of the raw key when split for the SFrame derivation; the
     * first half is the HKDF salt and the second half is the HKDF input keying material.
     */
    private static final int SFRAME_HALF = 16;

    /**
     * Holds the length, in bytes, of the first slice of the hop by hop key, used as the input keying
     * material for every hop by hop chaining salt derivation.
     */
    private static final int HBH_SALT_SECRET_LENGTH = 14;

    /**
     * Holds the length, in bytes, of the hop by hop chaining salt expanded from the salt secret and used
     * as the HKDF salt of the hop by hop key derivation.
     */
    private static final int HBH_CHAIN_SALT_LENGTH = 32;

    /**
     * Holds the all zero HKDF salt applied to the SRTP master and hop by hop chaining salt derivations,
     * matching the engine which passes a 32-byte zero salt (RFC 5869 "no salt").
     */
    private static final byte[] ZERO_SALT = new byte[32];

    /**
     * Holds the fixed ASCII label that prefixes the participant device JID in the SFrame HKDF info, with
     * no separator and no trailing {@code NUL} before the JID.
     */
    private static final byte[] SFRAME_INFO_LABEL = "e2e sframe key".getBytes(StandardCharsets.US_ASCII);

    /**
     * Enumerates the hop by hop key sets the SFU key derivation produces from the relay hop by hop key,
     * each identified by its ASCII info label prefix.
     *
     * <p>Every group is derived by the same two step HKDF chain over the {@code <hbh_key>} split (a
     * {@code "<prefix> salt"} chaining salt step followed by a {@code "<prefix> key"} master step); the
     * groups differ only in their prefix and in their key step output length, which
     * {@link #deriveHbhSrtpMaster(byte[], HopByHopGroup)} (the SRTP/SRTCP masters, 30 bytes) and
     * {@link #deriveWarpAuthKey(byte[])} (the WARP authentication key, 32 bytes) select. The end to end
     * {@code "e2e sframe key"} label is not a hop by hop group and is derived by
     * {@link #deriveSframeBaseKey(byte[], Jid)}, so it is deliberately absent here.
     */
    public enum HopByHopGroup {
        /**
         * The nondirectional hop by hop media SRTP key set, which protects the relayed media RTP on the
         * client to relay hop. A single master keys both legs because the relay forwards on one
         * hop by hop context.
         */
        MEDIA("hbh srtp"),

        /**
         * The base nondirectional hop by hop SRTCP key set, which keys the shared RTCP control traffic.
         */
        SRTCP("hbh srtcp"),

        /**
         * The uplink hop by hop SRTCP key set.
         */
        UPLINK_SRTCP("uplink hbh srtcp"),

        /**
         * The downlink hop by hop SRTCP key set.
         */
        DOWNLINK_SRTCP("downlink hbh srtcp"),

        /**
         * The hop by hop WARP authentication key set, which keys the HMAC SHA256 message integrity tag a
         * WARP control message carries toward the relay or SFU.
         *
         * <p>It is derived from the same relay {@code <hbh_key>} split as the SRTP and SRTCP groups, by the
         * same two step chain, but its key step output is the full {@value #WARP_AUTH_KEY_LENGTH}-byte raw
         * HMAC key rather than a 30-byte SRTP key plus salt; derive it through
         * {@link #deriveWarpAuthKey(byte[])}.
         */
        WARP_AUTH("warp auth");

        /**
         * Holds the ASCII label prefix of this group.
         */
        private final String label;

        /**
         * Constructs a group with its ASCII label prefix.
         *
         * @param label the ASCII label prefix, without the {@code " salt"} or {@code " key"} suffix
         */
        HopByHopGroup(String label) {
            this.label = label;
        }

        /**
         * Returns the HKDF info bytes for this group's chaining salt derivation.
         *
         * @return the ASCII bytes of {@code "<label> salt"}
         */
        private byte[] saltInfo() {
            return (label + " salt").getBytes(StandardCharsets.US_ASCII);
        }

        /**
         * Returns the HKDF info bytes for this group's keymat derivation.
         *
         * @return the ASCII bytes of {@code "<label> key"}
         */
        private byte[] keyInfo() {
            return (label + " key").getBytes(StandardCharsets.US_ASCII);
        }
    }

    /**
     * Prevents instantiation of this stateless derivation holder.
     */
    private CallE2eKeyDerivation() {
        throw new AssertionError("CallE2eKeyDerivation is not instantiable");
    }

    /**
     * Mints a fresh 32-byte raw end to end call key.
     *
     * <p>This is the per call secret the caller generates and distributes per device through the Signal
     * pipeline, and that a group endpoint mints again on a rekey. It is the input keying material every
     * other method in this chain expands.
     *
     * @return a new {@value #RAW_E2E_KEY_LENGTH}-byte cryptographically strong raw key
     * @implNote This implementation draws the bytes from {@link VoipCryptoNative#randomBytes(int)}.
     */
    public static byte[] mintRawKey() {
        var key = VoipCryptoNative.randomBytes(RAW_E2E_KEY_LENGTH);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "raw e2e call key minted {0}", key);
        return key;
    }

    /**
     * Validates a key generation version, accepting only {@value #SUPPORTED_KEYGEN_VER}.
     *
     * <p>Every derivation that consumes a {@code callKey} is gated on this check, so a participant whose
     * advertised keygen version is anything else produces no media keys and cannot be decrypted.
     *
     * @param keygenVer the key generation version recorded for the key material
     * @throws WhatsAppCallException.Srtp if {@code keygenVer} is not {@value #SUPPORTED_KEYGEN_VER}
     */
    public static void requireSupportedKeygenVersion(int keygenVer) {
        if (keygenVer != SUPPORTED_KEYGEN_VER) {
            throw new WhatsAppCallException.Srtp(
                    "Unsupported keygen ver " + keygenVer + ", only " + SUPPORTED_KEYGEN_VER + " is supported");
        }
    }

    /**
     * Derives the SFrame base key for one participant device from the raw call key.
     *
     * <p>The raw key is split into two 16-byte halves: the first half is the HKDF salt and the second
     * half is the HKDF input keying material. The info is the fixed {@code "e2e sframe key"} label
     * immediately followed by the participant's device JID rendered as raw ASCII (for example
     * {@code 83116928594056:2@lid}), with no separator and no trailing {@code NUL}. Because only the JID
     * varies between participant devices, each device of each participant receives a distinct base key
     * from the same call key.
     *
     * {@snippet :
     *   salt    = rawKey[0  .. 16]                       // first 16 bytes
     *   ikm     = rawKey[16 .. 32]                       // second 16 bytes
     *   info    = "e2e sframe key" + deviceJid           // ASCII, no separator, no NUL
     *   baseKey = HKDF-SHA256(ikm, salt, info, L = 32)
     * }
     *
     * @param rawKey    the {@value #RAW_E2E_KEY_LENGTH}-byte raw end to end call key
     * @param deviceJid the participant device whose base key is derived
     * @return the {@value #SFRAME_BASE_KEY_LENGTH}-byte SFrame base key for {@code deviceJid}
     * @throws NullPointerException       if {@code rawKey} or {@code deviceJid} is {@code null}
     * @throws IllegalArgumentException   if {@code rawKey} is not exactly {@value #RAW_E2E_KEY_LENGTH}
     *                                    bytes long
     * @throws WhatsAppCallException.Srtp if the HKDF computation fails
     * @implNote This implementation derives only the base (chain) key; the per frame key, salt, and nonce
     *           expansion and the chain ratchet that sit on top of it are owned by the SFrame
     *           media crypto layer.
     */
    public static byte[] deriveSframeBaseKey(byte[] rawKey, Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        requireRawKey(rawKey);
        var jidBytes = participantInfo(deviceJid).getBytes(StandardCharsets.US_ASCII);
        var info = new byte[SFRAME_INFO_LABEL.length + jidBytes.length];
        System.arraycopy(SFRAME_INFO_LABEL, 0, info, 0, SFRAME_INFO_LABEL.length);
        System.arraycopy(jidBytes, 0, info, SFRAME_INFO_LABEL.length, jidBytes.length);
        var salt = Arrays.copyOfRange(rawKey, 0, SFRAME_HALF);
        var ikm = Arrays.copyOfRange(rawKey, SFRAME_HALF, RAW_E2E_KEY_LENGTH);
        var baseKey = VoipCryptoNative.hkdfSha256(ikm, salt, info, SFRAME_BASE_KEY_LENGTH);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sframe base key derived for {0}", deviceJid);
        return baseKey;
    }

    /**
     * Derives the 30-byte per participant SRTP master from the raw call key.
     *
     * <p>The master is a single HKDF SHA256 expansion over the raw key with the participant device JID
     * as the info and the RFC 5869 no salt default (a 32-byte zero salt). The leading 16 bytes are the
     * {@code AES_CM_128_HMAC_SHA1} key and the trailing 14 bytes are the master salt; split them with
     * {@link #srtpMasterKey(byte[])} and {@link #srtpMasterSalt(byte[])}. A sender keys its outbound
     * stream from its own device JID and its inbound stream from the peer's device JID.
     *
     * {@snippet :
     *   master = HKDF-SHA256(ikm = rawKey, salt = 32 zero bytes, info = deviceJid, L = 30)
     * }
     *
     * @param rawKey    the {@value #RAW_E2E_KEY_LENGTH}-byte raw end to end call key
     * @param deviceJid the participant device whose SRTP master is derived
     * @return the {@value #SRTP_MASTER_LENGTH}-byte SRTP master for {@code deviceJid}
     * @throws NullPointerException       if {@code rawKey} or {@code deviceJid} is {@code null}
     * @throws IllegalArgumentException   if {@code rawKey} is not exactly {@value #RAW_E2E_KEY_LENGTH}
     *                                    bytes long
     * @throws WhatsAppCallException.Srtp if the HKDF computation fails
     */
    public static byte[] deriveSrtpMaster(byte[] rawKey, Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        requireRawKey(rawKey);
        var info = participantInfo(deviceJid).getBytes(StandardCharsets.US_ASCII);
        var master = VoipCryptoNative.hkdfSha256(rawKey, ZERO_SALT, info, SRTP_MASTER_LENGTH);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "srtp master derived for {0}", deviceJid);
        return master;
    }

    /**
     * Derives the complete media key chain for one participant device.
     *
     * <p>This is the single call form for the engine's per participant rekey: it validates the keygen
     * version, derives the SFrame base key and the SRTP master, and packages them in a
     * {@link ParticipantKeyChain}. It does not derive the hop by hop key set, which is keyed from the
     * separate relay {@code <hbh_key>} rather than the {@code callKey}; use
     * {@link #deriveHbhSrtpMaster(byte[], HopByHopGroup)} for that.
     *
     * @param rawKey    the {@value #RAW_E2E_KEY_LENGTH}-byte raw end to end call key
     * @param keygenVer the key generation version recorded for {@code rawKey}
     * @param deviceJid the participant device whose key chain is derived
     * @return the participant's SFrame and SRTP key material
     * @throws NullPointerException       if {@code rawKey} or {@code deviceJid} is {@code null}
     * @throws IllegalArgumentException   if {@code rawKey} is not exactly {@value #RAW_E2E_KEY_LENGTH}
     *                                    bytes long
     * @throws WhatsAppCallException.Srtp if {@code keygenVer} is unsupported or an HKDF computation fails
     */
    public static ParticipantKeyChain deriveKeyChain(byte[] rawKey, int keygenVer, Jid deviceJid) {
        requireSupportedKeygenVersion(keygenVer);
        var sframeBaseKey = deriveSframeBaseKey(rawKey, deviceJid);
        var srtpMaster = deriveSrtpMaster(rawKey, deviceJid);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "media key chain derived for {0}, keygenVer={1}", deviceJid, keygenVer);
        return new ParticipantKeyChain(sframeBaseKey, srtpMaster);
    }

    /**
     * Splits a 30-byte SRTP master into its {@code AES_CM_128_HMAC_SHA1} master key.
     *
     * @param master a {@value #SRTP_MASTER_LENGTH}-byte master from
     *               {@link #deriveSrtpMaster(byte[], Jid)}
     * @return the {@value #SRTP_DIRECTION_KEY_LENGTH}-byte master key
     * @throws NullPointerException     if {@code master} is {@code null}
     * @throws IllegalArgumentException if {@code master} is not exactly {@value #SRTP_MASTER_LENGTH}
     *                                  bytes
     */
    public static byte[] srtpMasterKey(byte[] master) {
        requireLength(master, SRTP_MASTER_LENGTH, "master");
        return Arrays.copyOfRange(master, 0, SRTP_DIRECTION_KEY_LENGTH);
    }

    /**
     * Splits a 30-byte SRTP master into its {@code AES_CM_128_HMAC_SHA1} master salt.
     *
     * @param master a {@value #SRTP_MASTER_LENGTH}-byte master from
     *               {@link #deriveSrtpMaster(byte[], Jid)}
     * @return the 14-byte master salt
     * @throws NullPointerException     if {@code master} is {@code null}
     * @throws IllegalArgumentException if {@code master} is not exactly {@value #SRTP_MASTER_LENGTH}
     *                                  bytes
     */
    public static byte[] srtpMasterSalt(byte[] master) {
        requireLength(master, SRTP_MASTER_LENGTH, "master");
        return Arrays.copyOfRange(master, SRTP_DIRECTION_KEY_LENGTH, SRTP_MASTER_LENGTH);
    }

    /**
     * Derives the 30-byte hop by hop SRTP or SRTCP master for a group from the relay hop by hop key.
     *
     * <p>The relay hop by hop key, unlike the {@code callKey}, comes from the base64 {@code <hbh_key>}
     * the relay hands out in the call ack, and keys only the client to relay leg. It is split into a
     * 14-byte salt secret and a 16-byte key secret, and each group is derived by a chained pair of
     * HKDF SHA256 computations: a 32-byte chaining salt is expanded from the salt secret under an
     * all zero salt, then the 30-byte master is expanded from the key secret using that chaining salt as
     * the HKDF salt:
     *
     * {@snippet :
     *   saltSecret = hbhKey[0  .. 14]   // 14 bytes
     *   keySecret  = hbhKey[14 .. 30]   // 16 bytes
     *   chainSalt  = HKDF-SHA256(ikm = saltSecret, salt = 32 zero bytes, info = "<group> salt", L = 32)
     *   master     = HKDF-SHA256(ikm = keySecret,  salt = chainSalt,     info = "<group> key",  L = 30)
     * }
     *
     * <p>The 30-byte master is an {@code AES_CM_128_HMAC_SHA1_80} key (16 bytes) concatenated with a
     * master salt (14 bytes); split it with {@link #srtpMasterKey(byte[])} and
     * {@link #srtpMasterSalt(byte[])}.
     *
     * @param hbhKey the {@value #HBH_KEY_LENGTH}-byte relay hop by hop key (the decoded {@code <hbh_key>})
     * @param group  the hop by hop key set to derive
     * @return the {@value #HBH_SRTP_MASTER_LENGTH}-byte hop by hop master
     * @throws NullPointerException       if {@code hbhKey} or {@code group} is {@code null}
     * @throws IllegalArgumentException   if {@code hbhKey} is not exactly {@value #HBH_KEY_LENGTH} bytes
     *                                    long
     * @throws WhatsAppCallException.Srtp if an HKDF computation fails
     */
    public static byte[] deriveHbhSrtpMaster(byte[] hbhKey, HopByHopGroup group) {
        Objects.requireNonNull(group, "group cannot be null");
        var master = deriveChainedHbhKey(hbhKey, group, HBH_SRTP_MASTER_LENGTH);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "hop-by-hop srtp master derived for group {0}", group);
        return master;
    }

    /**
     * Derives the {@value #WARP_AUTH_KEY_LENGTH}-byte hop by hop WARP authentication key from the relay
     * hop by hop key.
     *
     * <p>The WARP authentication key keys the HMAC SHA256 message integrity tag a WARP control message
     * carries toward the relay so the relay can authenticate the control without holding the call's
     * end to end keys. It is derived from the same {@value #HBH_KEY_LENGTH}-byte relay {@code <hbh_key>}
     * and by the same two step HKDF chain as the hop by hop SRTP and SRTCP masters
     * ({@link #deriveHbhSrtpMaster(byte[], HopByHopGroup)}), differing only in its {@link HopByHopGroup#WARP_AUTH}
     * label prefix and in its {@value #WARP_AUTH_KEY_LENGTH}-byte key step output (a raw HMAC key, with no
     * SRTP salt portion):
     *
     * {@snippet :
     *   saltSecret = hbhKey[0  .. 14]   // 14 bytes
     *   keySecret  = hbhKey[14 .. 30]   // 16 bytes
     *   chainSalt  = HKDF-SHA256(ikm = saltSecret, salt = 32 zero bytes, info = "warp auth salt", L = 32)
     *   warpAuth   = HKDF-SHA256(ikm = keySecret,  salt = chainSalt,     info = "warp auth key",  L = 32)
     * }
     *
     * <p>The derived key is the keying material {@code WarpMessageIntegrity} computes its tag under; it is
     * derived only when the relay enables WARP message integrity (a positive {@code warp_mi_tag_len} on the
     * relay block).
     *
     * @param hbhKey the {@value #HBH_KEY_LENGTH}-byte relay hop by hop key (the decoded {@code <hbh_key>})
     * @return the {@value #WARP_AUTH_KEY_LENGTH}-byte WARP authentication key
     * @throws NullPointerException       if {@code hbhKey} is {@code null}
     * @throws IllegalArgumentException   if {@code hbhKey} is not exactly {@value #HBH_KEY_LENGTH} bytes long
     * @throws WhatsAppCallException.Srtp if an HKDF computation fails
     */
    public static byte[] deriveWarpAuthKey(byte[] hbhKey) {
        var key = deriveChainedHbhKey(hbhKey, HopByHopGroup.WARP_AUTH, WARP_AUTH_KEY_LENGTH);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "warp auth key derived");
        return key;
    }

    /**
     * Runs the two step hop by hop HKDF chain for a group, expanding the key step output to a given length.
     *
     * <p>The relay {@code <hbh_key>} is split into a {@value #HBH_SALT_SECRET_LENGTH}-byte salt secret and a
     * key secret; a {@value #HBH_CHAIN_SALT_LENGTH}-byte chaining salt is expanded from the salt secret under
     * an all zero salt with the group's {@code "<prefix> salt"} info, then the master is expanded from the
     * key secret using that chaining salt as the HKDF salt with the group's {@code "<prefix> key"} info to
     * {@code keyLength} bytes.
     *
     * @param hbhKey    the {@value #HBH_KEY_LENGTH}-byte relay hop by hop key
     * @param group     the key set selecting the info label prefix
     * @param keyLength the key step output length, in bytes
     * @return the {@code keyLength}-byte derived key
     * @throws NullPointerException       if {@code hbhKey} is {@code null}
     * @throws IllegalArgumentException   if {@code hbhKey} is not exactly {@value #HBH_KEY_LENGTH} bytes long
     * @throws WhatsAppCallException.Srtp if an HKDF computation fails
     */
    private static byte[] deriveChainedHbhKey(byte[] hbhKey, HopByHopGroup group, int keyLength) {
        requireLength(hbhKey, HBH_KEY_LENGTH, "hbhKey");
        var saltSecret = Arrays.copyOfRange(hbhKey, 0, HBH_SALT_SECRET_LENGTH);
        var chainSalt = VoipCryptoNative.hkdfSha256(saltSecret, ZERO_SALT, group.saltInfo(), HBH_CHAIN_SALT_LENGTH);
        var keySecret = Arrays.copyOfRange(hbhKey, HBH_SALT_SECRET_LENGTH, HBH_KEY_LENGTH);
        return VoipCryptoNative.hkdfSha256(keySecret, chainSalt, group.keyInfo(), keyLength);
    }

    /**
     * Renders the participant identifier HKDF {@code info} string for a call device, always including the
     * device component even when it addresses the primary device.
     *
     * <p>Both the SFrame base key and the SRTP master derivations mix the participant's device qualified LID
     * into the HKDF info (for example {@code 83116928594056:2@lid}). The device suffix is mandatory: a
     * primary device is rendered {@code user:0@lid}, not the bare {@code user@lid}. This is where the E2E
     * info diverges from {@link Jid#toString()}, which omits a
     * {@code 0} device; keying a primary device peer's inbound stream from {@code user@lid} would feed a
     * different {@code info} into HKDF, produce a different SRTP master, and leave the peer's media
     * undecodable (decrypting to noise). A nonprimary device renders identically to the JID string form.
     *
     * @param deviceJid the participant device whose identifier is rendered
     * @return the ASCII participant identifier in the form {@code user[_agent]:device@server}
     */
    private static String participantInfo(Jid deviceJid) {
        var agent = deviceJid.agent() != 0 ? "_" + deviceJid.agent() : "";
        return deviceJid.user() + agent + ":" + deviceJid.device() + "@" + deviceJid.server();
    }

    /**
     * Validates a raw end to end key is not null and exactly {@value #RAW_E2E_KEY_LENGTH} bytes.
     *
     * @param rawKey the raw key to validate
     * @throws NullPointerException     if {@code rawKey} is {@code null}
     * @throws IllegalArgumentException if {@code rawKey} is not exactly {@value #RAW_E2E_KEY_LENGTH}
     *                                  bytes
     */
    private static void requireRawKey(byte[] rawKey) {
        requireLength(rawKey, RAW_E2E_KEY_LENGTH, "rawKey");
    }

    /**
     * Validates that a byte array is not null and has an exact length.
     *
     * @param value    the array to validate
     * @param expected the required length, in bytes
     * @param name     the parameter name for the failure messages
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not exactly {@code expected} bytes
     */
    private static void requireLength(byte[] value, int expected, String name) {
        Objects.requireNonNull(value, name + " cannot be null");
        if (value.length != expected) {
            throw new IllegalArgumentException(name + " must be " + expected + " bytes, got " + value.length);
        }
    }

    /**
     * Holds the per participant media key material derived from one raw end to end call key.
     *
     * <p>This is the local expansion of the single 32-byte wire key for one participant device: the
     * SFrame base key that seeds the end to end media crypto, and the SRTP master that seeds the
     * transport key set. The three {@link RekeyKeyType} domains (audio, video, AppData) are not separate
     * fields because the supported keygen version 2 path derives one domain agnostic SFrame base key and
     * one SRTP master per participant device; the per domain split, when a future keygen version
     * introduces it, is computed locally and never transmitted.
     *
     * <p>The arrays are defensively copied on construction and on access so this value object stays
     * immutable; the byte contents are equality relevant.
     *
     * @param sframeBaseKey the {@value #SFRAME_BASE_KEY_LENGTH}-byte SFrame base (chain) key
     * @param srtpMaster    the {@value #SRTP_MASTER_LENGTH}-byte SRTP master (16-byte key plus 14-byte
     *                      salt)
     */
    public record ParticipantKeyChain(byte[] sframeBaseKey, byte[] srtpMaster) {
        /**
         * Canonicalizes the record components, validating their lengths and defensively copying them.
         *
         * @throws NullPointerException     if {@code sframeBaseKey} or {@code srtpMaster} is {@code null}
         * @throws IllegalArgumentException if either array has the wrong length
         */
        public ParticipantKeyChain {
            requireLength(sframeBaseKey, SFRAME_BASE_KEY_LENGTH, "sframeBaseKey");
            requireLength(srtpMaster, SRTP_MASTER_LENGTH, "srtpMaster");
            sframeBaseKey = sframeBaseKey.clone();
            srtpMaster = srtpMaster.clone();
        }

        /**
         * Returns a copy of the SFrame base (chain) key.
         *
         * @return a {@value #SFRAME_BASE_KEY_LENGTH}-byte copy of the SFrame base key
         */
        @Override
        public byte[] sframeBaseKey() {
            return sframeBaseKey.clone();
        }

        /**
         * Returns a copy of the SRTP master (16-byte key followed by 14-byte salt).
         *
         * @return a {@value #SRTP_MASTER_LENGTH}-byte copy of the SRTP master
         */
        @Override
        public byte[] srtpMaster() {
            return srtpMaster.clone();
        }

        /**
         * Returns the {@code AES_CM_128_HMAC_SHA1} key half of the SRTP master.
         *
         * @return the {@value #SRTP_DIRECTION_KEY_LENGTH}-byte SRTP key
         */
        public byte[] srtpKey() {
            return CallE2eKeyDerivation.srtpMasterKey(srtpMaster);
        }

        /**
         * Returns the salt half of the SRTP master.
         *
         * @return the 14-byte SRTP salt
         */
        public byte[] srtpSalt() {
            return CallE2eKeyDerivation.srtpMasterSalt(srtpMaster);
        }

        /**
         * Compares this key chain with another for byte wise equality of both key arrays.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link ParticipantKeyChain} with the same SFrame base
         *         key and SRTP master bytes
         */
        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof ParticipantKeyChain that
                    && Arrays.equals(this.sframeBaseKey, that.sframeBaseKey)
                    && Arrays.equals(this.srtpMaster, that.srtpMaster));
        }

        /**
         * Returns a hash code derived from the byte contents of both key arrays.
         *
         * @return the content based hash code
         */
        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(sframeBaseKey) + Arrays.hashCode(srtpMaster);
        }

        /**
         * Returns a diagnostic string identifying this key chain by the lengths of its two arrays, never
         * their secret contents.
         *
         * @return a string carrying only the key array lengths
         */
        @Override
        public String toString() {
            return "ParticipantKeyChain[sframeBaseKeyLen=" + sframeBaseKey.length
                    + ", srtpMasterLen=" + srtpMaster.length + ']';
        }
    }
}
