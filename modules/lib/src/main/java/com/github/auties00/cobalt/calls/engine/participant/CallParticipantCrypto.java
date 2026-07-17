package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.calls.media.sframe.SFrameKeyProvider;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.E2eRekeyPayload;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RekeyKeyEntry;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RekeyKeyType;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.crypto.CallE2eKeyDerivation;

/**
 * Holds the per participant end to end crypto state of a call participant.
 *
 * <p>Every participant carries its own crypto block: the raw end to end key of
 * {@value #RAW_E2E_KEY_LENGTH} bytes the caller minted and distributed over the Signal
 * pipeline, the key generation version and bookkeeping the engine validates before accepting
 * that key, and the keys the engine derives from it (the SFrame chain key and the per
 * participant SRTP master). This class is both the data holder for that block and the entry
 * point that derives the per participant keys from the stored raw key:
 * {@link #deriveKeys(Jid)} runs the {@link CallE2eKeyDerivation} chain for the
 * participant's device JID and writes the SFrame chain key and the SRTP master back into the
 * holder. The lower level setters ({@link #sframeChainKey(byte[])}, {@link #srtpMaster(byte[])})
 * remain so a caller that derives elsewhere can install results directly.
 *
 * <p>The one normative behaviour the ingest enforces is the engine's contract for a raw key:
 * the raw key length must lie in {@code 1..}{@value #RAW_E2E_KEY_LENGTH} and the key generation
 * version must equal {@value #SUPPORTED_KEYGEN_VER}.
 * {@link #storeRawKey(byte[], int, int, boolean)} applies both checks and records the key,
 * version, key count, transaction id, and has bot flag together. Once a raw key is stored the
 * derived keys are cleared, because they no longer correspond to the new key until they are
 * derived again.
 *
 * <p>The SFrame chain key is derived per participant from the raw key bound to that
 * participant's device JID through {@link CallE2eKeyDerivation#deriveSframeBaseKey}; the
 * transaction id keys the SFrame key provider so that a repeated rekey for the same transaction
 * is idempotent. The SRTP master is the per participant {@code AES_CM_128_HMAC_SHA1} master (a
 * 16 byte key followed by a 14 byte salt) the transport keys its peer to peer SRTP context from.
 *
 * <p>This class is not thread safe; the engine mutates a participant's crypto block only while
 * holding the membership lock that guards the owning {@link CallParticipant}, and the derivation
 * itself runs under the call's stream lock. All key accessors return defensive copies so stored
 * key material cannot be mutated through a returned reference.
 */
public final class CallParticipantCrypto {
    /**
     * The logger for {@link CallParticipantCrypto}.
     */
    private static final System.Logger LOGGER = Log.get(CallParticipantCrypto.class);

    /**
     * Holds the required length, in bytes, of the raw end to end key.
     *
     * <p>The engine mints exactly this many random bytes for the call key and rejects an
     * ingested key longer than this.
     */
    public static final int RAW_E2E_KEY_LENGTH = 32;

    /**
     * Holds the only key generation version the engine accepts.
     *
     * <p>An ingested key carrying any other version is rejected.
     */
    public static final int SUPPORTED_KEYGEN_VER = 2;

    /**
     * Holds the length, in bytes, of the derived SFrame chain key.
     */
    public static final int SFRAME_CHAIN_KEY_LENGTH = 32;

    /**
     * Holds the length, in bytes, of the derived per participant SRTP master (a 16 byte
     * {@code AES_CM_128_HMAC_SHA1} key followed by a 14 byte salt).
     */
    public static final int SRTP_MASTER_LENGTH = 30;

    /**
     * Holds the ASCII label that prefixes the SFrame chain key HKDF info string.
     *
     * <p>The crypto layer builds the HKDF info as these fourteen ASCII bytes immediately
     * followed by the participant's device JID rendered as raw ASCII, with no separator and
     * no NUL terminator.
     */
    public static final String SFRAME_HKDF_INFO_LABEL = "e2e sframe key";

    /**
     * Holds the sentinel transaction id meaning no rekey transaction has been recorded.
     */
    public static final int NO_TRANSACTION_ID = -1;

    /**
     * Holds the raw end to end key, or {@code null} until a key has been ingested.
     */
    private byte[] rawKey;

    /**
     * Holds the key generation version recorded with the current raw key.
     */
    private int keygenVersion;

    /**
     * Holds the key count recorded with the current raw key.
     */
    private int keyCount;

    /**
     * Holds the rekey transaction id recorded with the current raw key, or
     * {@value #NO_TRANSACTION_ID} when none has been recorded.
     */
    private int transactionId;

    /**
     * Holds whether the current raw key was ingested for a participant that has a bot.
     */
    private boolean hasBot;

    /**
     * Holds the derived SFrame chain key, or {@code null} until the crypto layer derives
     * it.
     */
    private byte[] sframeChainKey;

    /**
     * Holds the derived per participant SRTP master (16 byte key plus 14 byte salt), or
     * {@code null} until it is derived.
     */
    private byte[] srtpMaster;

    /**
     * Holds whether the self SRTP and SFrame transmit update has completed for the current
     * key.
     */
    private boolean transmitKeysInstalled;

    /**
     * Constructs an empty crypto block with no raw key, no derived keys, and no recorded
     * transaction.
     */
    public CallParticipantCrypto() {
        this.transactionId = NO_TRANSACTION_ID;
    }

    /**
     * Stores a freshly ingested raw end to end key together with its bookkeeping.
     *
     * <p>This applies the engine's ingest contract: the raw key length must lie in
     * {@code 1..}{@value #RAW_E2E_KEY_LENGTH} and {@code keygenVersion} must equal
     * {@value #SUPPORTED_KEYGEN_VER}. On success it records a defensive copy of the key, the
     * version, the key count, the transaction id, and the has bot flag, and clears any
     * previously derived keys, since they no longer correspond to the new raw key. On failure
     * it throws and leaves the previous state untouched.
     *
     * @param rawKey        the raw end to end key bytes; never {@code null}
     * @param keygenVersion the key generation version; must equal {@value #SUPPORTED_KEYGEN_VER}
     * @param keyCount      the key count the engine recorded for this key
     * @param hasBot        whether the participant has a bot
     * @throws WhatsAppCallException.Srtp if {@code rawKey} is {@code null}, its length is outside
     *                                    {@code 1..}{@value #RAW_E2E_KEY_LENGTH}, or the keygen
     *                                    version is unsupported
     */
    public void storeRawKey(byte[] rawKey, int keygenVersion, int keyCount, boolean hasBot) {
        storeRawKey(rawKey, keygenVersion, keyCount, hasBot, NO_TRANSACTION_ID);
    }

    /**
     * Stores a freshly ingested raw end to end key, its bookkeeping, and the rekey transaction
     * id that delivered it.
     *
     * <p>This behaves exactly like {@link #storeRawKey(byte[], int, int, boolean)} but also
     * records the transaction id that keys the SFrame key provider, so a later rekey for the
     * same transaction can be recognized as a duplicate.
     *
     * @param rawKey        the raw end to end key bytes; never {@code null}
     * @param keygenVersion the key generation version; must equal {@value #SUPPORTED_KEYGEN_VER}
     * @param keyCount      the key count the engine recorded for this key
     * @param hasBot        whether the participant has a bot
     * @param transactionId the rekey transaction id, or {@value #NO_TRANSACTION_ID} if none
     * @throws WhatsAppCallException.Srtp if {@code rawKey} is {@code null}, its length is outside
     *                                    {@code 1..}{@value #RAW_E2E_KEY_LENGTH}, or the keygen
     *                                    version is unsupported
     */
    public void storeRawKey(byte[] rawKey, int keygenVersion, int keyCount, boolean hasBot, int transactionId) {
        if (rawKey == null) {
            throw new WhatsAppCallException.Srtp("raw E2E key cannot be null");
        }
        if (rawKey.length < 1 || rawKey.length > RAW_E2E_KEY_LENGTH) {
            throw new WhatsAppCallException.Srtp(
                    "raw E2E key length " + rawKey.length + " is outside the range 1.." + RAW_E2E_KEY_LENGTH);
        }
        if (keygenVersion != SUPPORTED_KEYGEN_VER) {
            throw new WhatsAppCallException.Srtp("Unsupported keygen ver " + keygenVersion);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "storing raw call key {0}, keygenVer={1}, keyCount={2}, transactionId={3}",
                    rawKey, keygenVersion, keyCount, transactionId);
        }
        this.rawKey = rawKey.clone();
        this.keygenVersion = keygenVersion;
        this.keyCount = keyCount;
        this.hasBot = hasBot;
        this.transactionId = transactionId;
        this.sframeChainKey = null;
        this.srtpMaster = null;
        this.transmitKeysInstalled = false;
    }

    /**
     * Stores the raw end to end key carried by an inbound rekey payload.
     *
     * <p>A rekey payload carries a single raw key of {@value #RAW_E2E_KEY_LENGTH} bytes (inside a
     * {@code Message.Call.callKey}), not three pre derived per domain masters; the three per domain
     * keys ({@link RekeyKeyType#AUDIO}, {@link RekeyKeyType#VIDEO}, {@link RekeyKeyType#APPDATA}) are
     * derived locally from this one key. This convenience extracts that single key from a single
     * entry {@link E2eRekeyPayload} and ingests it through
     * {@link #storeRawKey(byte[], int, int, boolean, int)}. It requires the payload to carry exactly
     * one {@link RekeyKeyEntry} and rejects a payload with any other count rather than silently
     * dropping all but the first entry.
     *
     * @param payload       the decoded rekey payload; never {@code null} and carrying exactly one
     *                      key entry
     * @param keyCount      the key count to record for the rekeyed key
     * @param hasBot        whether the participant has a bot
     * @param transactionId the rekey transaction id, or {@value #NO_TRANSACTION_ID} if none
     * @throws WhatsAppCallException.Srtp if {@code payload} is {@code null}, does not carry exactly
     *                                    one key entry, or the key fails the ingest contract
     */
    public void storeRekeyPayload(E2eRekeyPayload payload, int keyCount, boolean hasBot, int transactionId) {
        if (payload == null) {
            throw new WhatsAppCallException.Srtp("rekey payload cannot be null");
        }
        var keys = payload.keys();
        // TODO: only the single key rekey shape has been observed; whether a multi entry payload
        //  of per domain masters can occur (for example on a group call rekey) is unconfirmed.
        if (keys.size() != 1) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "rekey payload carries {0} key entries, expected exactly 1, transactionId={1}",
                        keys.size(), transactionId);
            }
            throw new WhatsAppCallException.Srtp(
                    "rekey payload must carry exactly one key entry, got " + keys.size());
        }
        storeRawKey(keys.getFirst().key(), SUPPORTED_KEYGEN_VER, keyCount, hasBot, transactionId);
    }

    /**
     * Returns whether a raw end to end key has been ingested.
     *
     * @return {@code true} if a raw key is present
     */
    public boolean hasRawKey() {
        return rawKey != null;
    }

    /**
     * Returns the raw end to end key.
     *
     * <p>The returned array, when present, is a defensive copy; mutating it does not affect
     * this holder.
     *
     * @return an {@code Optional} holding a copy of the raw key, or empty if no key has
     *         been ingested
     */
    public Optional<byte[]> rawKey() {
        return Optional.ofNullable(rawKey)
                .map(byte[]::clone);
    }

    /**
     * Returns the key generation version recorded with the current raw key.
     *
     * @return the keygen version, or {@code 0} if no key has been ingested
     */
    public int keygenVersion() {
        return keygenVersion;
    }

    /**
     * Returns the key count recorded with the current raw key.
     *
     * @return the key count, or {@code 0} if no key has been ingested
     */
    public int keyCount() {
        return keyCount;
    }

    /**
     * Returns the rekey transaction id recorded with the current raw key.
     *
     * @return the transaction id, or {@value #NO_TRANSACTION_ID} if none has been recorded
     */
    public int transactionId() {
        return transactionId;
    }

    /**
     * Returns whether the current raw key was ingested for a participant that has a bot.
     *
     * @return {@code true} if the participant has a bot
     */
    public boolean hasBot() {
        return hasBot;
    }

    /**
     * Records the SFrame chain key the crypto layer derived for this participant.
     *
     * <p>The chain key is the 32 byte output of the SFrame HKDF over this participant's raw key,
     * bound to the participant's device JID. This holder stores a defensive copy; it does not
     * perform the derivation.
     *
     * @param sframeChainKey the derived SFrame chain key; never {@code null}
     * @throws WhatsAppCallException.Srtp if {@code sframeChainKey} is {@code null} or is not
     *                                    {@value #SFRAME_CHAIN_KEY_LENGTH} bytes
     */
    public void sframeChainKey(byte[] sframeChainKey) {
        if (sframeChainKey == null) {
            throw new WhatsAppCallException.Srtp("SFrame chain key cannot be null");
        }
        if (sframeChainKey.length != SFRAME_CHAIN_KEY_LENGTH) {
            throw new WhatsAppCallException.Srtp(
                    "SFrame chain key length " + sframeChainKey.length + " must be " + SFRAME_CHAIN_KEY_LENGTH);
        }
        this.sframeChainKey = sframeChainKey.clone();
    }

    /**
     * Returns the SFrame chain key the crypto layer derived for this participant.
     *
     * <p>The returned array, when present, is a defensive copy.
     *
     * @return an {@code Optional} holding a copy of the SFrame chain key, or empty if it
     *         has not been derived
     */
    public Optional<byte[]> sframeChainKey() {
        return Optional.ofNullable(sframeChainKey)
                .map(byte[]::clone);
    }

    /**
     * Installs this participant's derived SFrame chain key into a key provider.
     *
     * <p>This is the seam the media plane drives to make a participant's relayed SFrame media
     * decryptable: the provider it hands an inbound demux for this participant receives this
     * participant's chain key (its SFrame base key), keyed by the recorded
     * {@linkplain #transactionId() rekey transaction id}, or {@code 0} when no rekey transaction has
     * been recorded. Installing a chain key for a transaction id already present in the provider does
     * nothing, so a repeated rekey delivery is idempotent. Returns {@code false} (and installs
     * nothing) when this block has no derived chain key yet, so the caller can skip a participant
     * whose keys are not ready.
     *
     * @implNote This implementation installs the derived base key under the rekey transaction id; the
     * frame cipher for a received frame is then resolved by that frame's own key id, not by the
     * transaction id.
     * @param provider the SFrame key provider to install the chain key into; never {@code null}
     * @return {@code true} if a chain key was installed, {@code false} if none is derived or the
     *         transaction was already present
     * @throws NullPointerException if {@code provider} is {@code null}
     */
    public boolean installSframeChainKey(SFrameKeyProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        if (sframeChainKey == null) {
            return false;
        }
        var keyTransaction = transactionId == NO_TRANSACTION_ID ? 0L : transactionId;
        var installed = provider.setChainKey(sframeChainKey, keyTransaction);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "installed sframe chain key for transaction {0}: {1}", keyTransaction, installed);
        }
        return installed;
    }

    /**
     * Records the per participant SRTP master the crypto layer derived for this participant.
     *
     * <p>The master is the thirty byte {@code AES_CM_128_HMAC_SHA1} master (16 byte key followed
     * by 14 byte salt) derived from the raw key bound to this participant's device JID. This holder
     * stores a defensive copy; it does not perform the derivation unless
     * {@link #deriveKeys(Jid)} is used.
     *
     * @implNote The transport keys its two SRTP directions from the 16 byte key halves of this
     * participant's and the peer participant's masters, one master per direction, rather than from
     * two keys packed into a single block.
     * @param srtpMaster the derived SRTP master; never {@code null}
     * @throws WhatsAppCallException.Srtp if {@code srtpMaster} is {@code null} or is not
     *                                    {@value #SRTP_MASTER_LENGTH} bytes
     */
    public void srtpMaster(byte[] srtpMaster) {
        if (srtpMaster == null) {
            throw new WhatsAppCallException.Srtp("SRTP master cannot be null");
        }
        if (srtpMaster.length != SRTP_MASTER_LENGTH) {
            throw new WhatsAppCallException.Srtp(
                    "SRTP master length " + srtpMaster.length + " must be " + SRTP_MASTER_LENGTH);
        }
        this.srtpMaster = srtpMaster.clone();
    }

    /**
     * Returns the per participant SRTP master.
     *
     * <p>The returned array, when present, is a defensive copy of the thirty byte master (16 byte
     * {@code AES_CM_128_HMAC_SHA1} key followed by 14 byte salt). Split it with
     * {@link CallE2eKeyDerivation#srtpMasterKey(byte[])} and
     * {@link CallE2eKeyDerivation#srtpMasterSalt(byte[])}.
     *
     * @return an {@code Optional} holding a copy of the SRTP master, or empty if it has not
     *         been derived
     */
    public Optional<byte[]> srtpMaster() {
        return Optional.ofNullable(srtpMaster)
                .map(byte[]::clone);
    }

    /**
     * Derives this participant's SFrame chain key and SRTP master from the stored raw key and
     * the participant's device JID, then records both in this block.
     *
     * <p>This is the holder driven form of the engine's inline per participant derivation: it
     * requires a raw key to have been ingested, runs the {@link CallE2eKeyDerivation} chain for
     * {@code deviceJid} under the stored {@linkplain #keygenVersion() keygen version}, and stores
     * the resulting SFrame base (chain) key and SRTP master through {@link #sframeChainKey(byte[])}
     * and {@link #srtpMaster(byte[])}. The SFrame chain key is the one the media plane installs into
     * this participant's {@link SFrameKeyProvider} so its relayed media decrypts.
     *
     * <p>The raw key must be the full {@value #RAW_E2E_KEY_LENGTH} bytes for the chain, since
     * {@link CallE2eKeyDerivation} requires a full length key; a shorter ingested key (accepted by
     * {@link #storeRawKey(byte[], int, int, boolean)} for the {@code 1..32} contract) cannot be
     * expanded and is rejected here.
     *
     * @param deviceJid the participant's device JID binding the derivation; never {@code null}
     * @throws NullPointerException       if {@code deviceJid} is {@code null}
     * @throws WhatsAppCallException.Srtp if no raw key has been ingested, the keygen version is
     *                                    unsupported, or the raw key is not the full
     *                                    {@value #RAW_E2E_KEY_LENGTH} bytes
     */
    public void deriveKeys(Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        if (rawKey == null) {
            throw new WhatsAppCallException.Srtp("cannot derive keys before a raw key is ingested");
        }
        if (rawKey.length != RAW_E2E_KEY_LENGTH) {
            throw new WhatsAppCallException.Srtp(
                    "cannot derive keys from a " + rawKey.length + "-byte raw key, " + RAW_E2E_KEY_LENGTH
                            + " required");
        }
        var chain = CallE2eKeyDerivation.deriveKeyChain(rawKey, keygenVersion, deviceJid);
        sframeChainKey(chain.sframeBaseKey());
        srtpMaster(chain.srtpMaster());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "derived sframe and srtp keys for device {0}", deviceJid);
    }

    /**
     * Returns whether the self SRTP and SFrame transmit update has completed for the current
     * key.
     *
     * @return {@code true} if the transmit keys have been installed into the media plane
     */
    public boolean transmitKeysInstalled() {
        return transmitKeysInstalled;
    }

    /**
     * Marks whether the self SRTP and SFrame transmit update has completed for the current key.
     *
     * <p>The flag is set once the self transmit key push to the media plane succeeds, and is
     * cleared whenever a new raw key is ingested.
     *
     * @param transmitKeysInstalled {@code true} once the transmit keys are installed
     * @return this crypto block
     */
    public CallParticipantCrypto transmitKeysInstalled(boolean transmitKeysInstalled) {
        this.transmitKeysInstalled = transmitKeysInstalled;
        return this;
    }

    /**
     * Builds the SFrame chain key HKDF info bytes for the given device JID.
     *
     * <p>The info is the {@value #SFRAME_HKDF_INFO_LABEL} label rendered as ASCII, immediately
     * followed by the device JID rendered as raw ASCII, with no separator and no NUL terminator.
     * This is the exact info string the crypto layer must pass to the SFrame HKDF; it is exposed
     * here so the holder and the derivation engine agree on the derivation context.
     *
     * @param deviceJid the participant's device JID whose ASCII form binds the derivation; never
     *                  {@code null}
     * @return the HKDF info bytes
     * @throws WhatsAppCallException.Srtp if {@code deviceJid} is {@code null}
     */
    public static byte[] sframeHkdfInfo(Jid deviceJid) {
        if (deviceJid == null) {
            throw new WhatsAppCallException.Srtp("deviceJid cannot be null");
        }
        var label = SFRAME_HKDF_INFO_LABEL.getBytes(StandardCharsets.US_ASCII);
        var jid = deviceJid.toString().getBytes(StandardCharsets.US_ASCII);
        var info = new byte[label.length + jid.length];
        System.arraycopy(label, 0, info, 0, label.length);
        System.arraycopy(jid, 0, info, label.length, jid.length);
        return info;
    }

    @Override
    public String toString() {
        return "CallParticipantCrypto[hasRawKey=" + (rawKey != null)
                + ", keygenVersion=" + keygenVersion
                + ", keyCount=" + keyCount
                + ", transactionId=" + transactionId
                + ", hasBot=" + hasBot
                + ", sframeChainKeyDerived=" + (sframeChainKey != null)
                + ", srtpMasterDerived=" + (srtpMaster != null)
                + ", transmitKeysInstalled=" + transmitKeysInstalled
                + ']';
    }
}
