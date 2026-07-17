package com.github.auties00.cobalt.calls.crypto;

import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentitySpec;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.call.CallOfferMessage;
import com.github.auties00.cobalt.wire.linked.message.call.CallOfferMessageBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Mints, distributes, and recovers the thirty two byte end to end call key through the reused Signal
 * message encryption pipeline.
 *
 * <p>Every call protects its media with a single thirty two byte raw end to end key (Cobalt's
 * {@code callKey}; the WASM engine's {@code raw_e2e} key). This key is the ONLY secret the call plane
 * distributes over the wire; the per direction SRTP keys, the per participant SFrame base keys, and the
 * data channel certificate fingerprint HMAC are all derived LOCALLY from it via HKDF and never travel.
 * This service owns the four operations that move that key:
 * <ul>
 *   <li><b>mint</b> ({@link #mintCallKey()}): draw thirty two cryptographically strong random bytes.</li>
 *   <li><b>wrap</b> ({@link #wrapCallKey(byte[])}): encode the key as the plaintext
 *       {@code LinkedMessageContainer{Call{callKey}}}.</li>
 *   <li><b>distribute</b>: encrypt the wrapped plaintext per recipient device through the existing
 *       Signal session cipher, fanned out as a {@link #encryptOfferFanout(Collection, byte[]) per-device
 *       offer block} (1:1) or a {@link #encryptRekeyFanout(Collection, byte[]) per-recipient rekey
 *       fanout} (group), with the all or nothing bare destination fallback.</li>
 *   <li><b>recover</b> ({@link #decryptCallKey(Jid, MessageEncryptionType, byte[])}): Signal decrypt an
 *       inbound {@code <enc>} or {@code <enc_rekey>} envelope back to the thirty two byte key.</li>
 * </ul>
 *
 * <p>This service is a thin facade: it holds NO Signal cipher of its own. Every encrypt routes through
 * {@link MessageEncryption#encryptForDevice(Jid, byte[])} and every decrypt through
 * {@link MessageService#processCall(Jid, MessageEncryptionType, byte[])}, both of which already acquire
 * the per address lock in {@code SignalCryptoLocks}. Constructing a private
 * {@code SignalSessionCipher} or {@code SignalGroupCipher} here would bypass that lock and race the
 * non atomic Signal ratchet against concurrent message plane traffic on the same device session, so
 * this service deliberately never does so. The only call plane owned logic is the key minting, the
 * {@code LinkedMessageContainer{Call{callKey}}} wrap, the all or nothing bare destination fallback, the
 * group rekey fanout shape, and the dual inbound {@code <enc>} addressing shapes.
 *
 * <p>Group versus one to one: SFrame, and therefore on the wire rekeys, are GROUP only. A 1:1 call
 * ships the key once in the offer and never rekeys; a group call ships no key in the offer (the key
 * arrives post join via {@code <enc_rekey>}) and reshares a fresh key on every membership change, each
 * connected participant fanning its own key unicast to every other participant.
 *
 * @implNote This implementation wraps the raw key as a {@code LinkedMessageContainer} whose only populated
 * payload is a {@link CallOfferMessage} holding the key in its {@code callKey} field; the protobuf
 * encoding is {@code 52 22 0a 20 <32B callKey> ...} ({@code LinkedMessageContainer.call} at field 10 wrapping
 * {@code Call.callKey} at field 1). The same single thirty two byte plaintext serves both the 1:1 offer
 * key and the group rekey key; the per direction SRTP, per participant SFrame, and data channel keys are
 * all derived locally after decrypt and never transmitted. The group rekey reshares one key unicast to
 * each other connected participant rather than broadcasting per domain masters. The decrypt path is the
 * exact inverse: Signal decrypt, decode {@code LinkedMessageContainer}, read {@code Call.callKey}.
 */
public final class LiveCallKeyExchange implements CallKeyExchange {
    /**
     * The logger for {@link LiveCallKeyExchange}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallKeyExchange.class);

    /**
     * Holds the length, in bytes, of the raw end to end call key.
     *
     * <p>The key is exactly thirty two bytes; the engine rejects any recovered key longer than this.
     */
    public static final int CALL_KEY_LENGTH = 32;

    /**
     * The wire attribute naming the call identifier, stamped on every action element.
     *
     * <p>Shared with {@link CallRekeyEnvelope} so the rekey stanza and this service stamp the same literal.
     */
    static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The wire attribute naming the call creator's device JID, stamped on every action element.
     */
    static final String CALL_CREATOR_ATTRIBUTE = "call-creator";

    /**
     * The wire attribute naming the Signal ciphertext type on an {@code <enc>} element.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * The retry count value stamped on every offer call key {@code <enc>} element.
     *
     * <p>Every per device offer {@code <enc>} carries {@code count="0"}.
     */
    private static final int ENC_COUNT = 0;

    /**
     * Holds the encryption service used to wrap the call key per recipient device.
     */
    private final MessageEncryption encryption;

    /**
     * Holds the message service whose {@link MessageService#processCall(Jid, MessageEncryptionType, byte[])}
     * decrypts inbound call key envelopes.
     */
    private final MessageService messageService;

    /**
     * Holds the device service used to establish Signal sessions before encrypting the call key to each
     * recipient device.
     */
    private final DeviceService deviceService;

    /**
     * Holds the store consulted for the local ADV signed device identity attached to {@code pkmsg}
     * envelopes.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the cryptographically strong random source that mints the call key.
     */
    private final SecureRandom secureRandom;

    /**
     * Constructs a call key crypto facade bound to the reused Signal pipeline.
     *
     * <p>All four collaborators must share the same client store as the rest of the Signal pipeline so
     * the per address lock registry and session state observed during a call key encrypt or decrypt are
     * the same ones the message plane uses.
     *
     * @param encryption     the message encryption service used to wrap the call key per device
     * @param messageService the message service used to decrypt inbound call key envelopes
     * @param deviceService  the device service used to ensure Signal sessions before encryption
     * @param store          the store supplying the local ADV signed device identity
     * @param secureRandom   the cryptographically strong random source used to mint the call key
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveCallKeyExchange(MessageEncryption encryption, MessageService messageService,
                              DeviceService deviceService, LinkedWhatsAppStore store, SecureRandom secureRandom) {
        this.encryption = Objects.requireNonNull(encryption, "encryption cannot be null");
        this.messageService = Objects.requireNonNull(messageService, "messageService cannot be null");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService cannot be null");
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom cannot be null");
    }

    /**
     * Mints a fresh thirty two byte raw end to end call key.
     *
     * <p>The key is drawn from the injected {@link SecureRandom}; it is the only secret the call plane
     * distributes over the wire. The caller stores it on the call runtime so accept and rekey reuse it
     * and feed it to the local HKDF chain that derives the SRTP, SFrame, and data channel keys.
     *
     * @return a new {@value #CALL_KEY_LENGTH} byte call key
     */
    @Override
    public byte[] mintCallKey() {
        var callKey = new byte[CALL_KEY_LENGTH];
        secureRandom.nextBytes(callKey);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call key minted {0}", callKey);
        return callKey;
    }

    /**
     * Wraps a raw call key as the plaintext that travels Signal encrypted inside an {@code <enc>}.
     *
     * <p>The plaintext is a {@link LinkedMessageContainer} whose only populated payload is a
     * {@link CallOfferMessage} carrying the key in its {@code callKey} field. The returned bytes are the
     * protobuf encoding of that container; the caller hands them to a per device Signal encrypt. This is
     * the identical plaintext shape used for both the offer key and the group rekey key.
     *
     * @implNote This implementation produces the byte shape {@code 52 22 0a 20 <32B callKey> ...}: the
     * protobuf encoding of {@code LinkedMessageContainer.call} (field 10) wrapping {@code Call.callKey}
     * (field 1, thirty two bytes).
     *
     * @param callKey the raw call key to wrap
     * @return the protobuf encoded {@code LinkedMessageContainer{Call{callKey}}} plaintext
     * @throws NullPointerException     if {@code callKey} is {@code null}
     * @throws IllegalArgumentException if {@code callKey} is not {@value #CALL_KEY_LENGTH} bytes long
     */
    @Override
    public byte[] wrapCallKey(byte[] callKey) {
        Objects.requireNonNull(callKey, "callKey cannot be null");
        if (callKey.length != CALL_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "callKey must be " + CALL_KEY_LENGTH + " bytes, got " + callKey.length);
        }
        var callOffer = new CallOfferMessageBuilder()
                .callKey(callKey)
                .build();
        var container = new LinkedMessageContainerBuilder()
                .call(callOffer)
                .build();
        return LinkedMessageContainerSpec.encode(container);
    }

    /**
     * Encrypts a wrapped call key to every peer device as a one to one offer's per device fanout.
     *
     * <p>Each peer device becomes one {@link CallKeyDistribution} slot. The Signal session for every
     * device is established first; the existing session for every device is then dropped and
     * reestablished so a peer holding a stale or one sided session receives a clean, decryptable
     * envelope rather than silently rejecting the offer. The fanout is all or nothing: if encryption
     * fails for ANY device, every encrypted slot is discarded and each device is addressed with a
     * {@linkplain CallKeyDistribution#bare(Jid) bare destination} carrying no key, so the call still
     * rings.
     *
     * <p>The thirty two byte key never leaves this method except as a Signal ciphertext; the SKMSG
     * sender key envelope is never used for the call key fanout, even for group calls.
     *
     * @implNote This implementation forces {@link DeviceService#ensureSessions(Collection, boolean)} so a
     * one sided session is repaired, then runs a per device
     * {@link MessageEncryption#encryptForDevice(Jid, byte[])} (which acquires the
     * {@code SignalCryptoLocks} session lock), falling back to the
     * {@link MessageEncryptedPayload#bareDestination(Jid)} all or nothing marker. Each encrypted slot
     * carries the Signal ciphertext version {@value MessageEncryption#CIPHERTEXT_VERSION}, the
     * per ciphertext {@code msg}/{@code pkmsg} type, and the {@value #ENC_COUNT} retry count.
     *
     * @param deviceJids the peer device JIDs to fan the key out to; never empty in practice
     * @param plaintext  the wrapped {@code LinkedMessageContainer{Call{callKey}}} plaintext from
     *                   {@link #wrapCallKey(byte[])}
     * @return one fanout slot per device, all encrypted on success or all bare on any encryption failure
     * @throws NullPointerException if {@code deviceJids} or {@code plaintext} is {@code null}, or if
     *                              {@code deviceJids} contains a {@code null} element
     */
    @Override
    public List<CallKeyDistribution> encryptOfferFanout(Collection<Jid> deviceJids, byte[] plaintext) {
        Objects.requireNonNull(deviceJids, "deviceJids cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        var devices = List.copyOf(deviceJids);

        // A device that already holds a healthy session must still be reestablished here because a stale
        // session makes the peer silently drop the offer rather than reject it.
        deviceService.ensureSessions(devices, true);

        var slots = new ArrayList<CallKeyDistribution>(devices.size());
        var encryptionFailed = false;
        // WA best effort encrypts every device and never breaks early: WAWebVoipSendSignalingXmpp fanOutOffer
        // maps the per device encrypt over all <destination> children via Promise.all
        // (WAWebVoipWapNodeUtils.mapVoipWapChildrenAsync), catches per device, and on any failure strips every
        // <enc> for a bare fanout. Encrypting all devices here therefore matches WA; the wasted advances on the
        // shared Signal sessions are WA faithful (WA does not roll them back, it only skips the disk flush) and
        // are absorbed by Signal's skipped message key handling. Do not add an early break.
        for (var deviceJid : devices) {
            try {
                var payload = encryption.encryptForDevice(deviceJid, plaintext);
                slots.add(toFanoutSlot(payload));
            } catch (RuntimeException e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "call key encryption failed for " + new LogRedactable.User(String.valueOf(deviceJid))
                                    + "; stripping all <enc> for a bare fanout",
                            e);
                }
                encryptionFailed = true;
            }
        }
        if (encryptionFailed) {
            slots.clear();
            for (var deviceJid : devices) {
                slots.add(CallKeyDistribution.bare(deviceJid));
            }
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "offer fanout downgraded to bare for {0} devices", devices.size());
            }
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "offer fanout encrypted for {0} devices", slots.size());
        }
        return slots;
    }

    /**
     * Encrypts a wrapped call key to every connected participant device as a group rekey fanout.
     *
     * <p>This is the group only key rotation: the local participant mints one fresh key and reshares it
     * to every other connected participant device. Each recipient device becomes one
     * {@link CallRekeyEnvelope}, addressed unicast; the caller wraps each envelope in its own
     * {@code <call to="<recipientDeviceLid>"><enc_rekey>...} stanza with a per round transaction id.
     * Existing sessions are reused (no force): a rekey continues each device's ratchet rather than
     * bootstrapping a fresh one, and a {@code pkmsg} envelope carries the local device identity so a
     * peer lacking the session can still establish it.
     *
     * <p>Unlike the offer fanout there is no all or nothing fallback: a rekey that fails for one
     * recipient simply omits that recipient's envelope; the others still rotate. A device whose
     * encryption fails is skipped with a logged warning.
     *
     * @implNote This implementation emits one {@code <enc>} per recipient stanza (NOT a
     * {@code <destination>} block), an {@code <encopt keygen="2"/>} sibling, and a
     * {@code <device identity>} only on {@code pkmsg}. The plaintext is the SAME single thirty two byte
     * {@code LinkedMessageContainer{Call{callKey}}} as the offer key; the per direction keys are derived
     * locally after decrypt, not transmitted. The per device encrypt acquires the
     * {@code SignalCryptoLocks} session lock through {@link MessageEncryption#encryptForDevice(Jid, byte[])}.
     *
     * @param recipientDevices the connected participant devices to reshare the key to
     * @param plaintext        the wrapped {@code LinkedMessageContainer{Call{callKey}}} plaintext from
     *                         {@link #wrapCallKey(byte[])} for the freshly minted rekey key
     * @return one rekey envelope per recipient whose encryption succeeded, in input order
     * @throws NullPointerException if {@code recipientDevices} or {@code plaintext} is {@code null}, or
     *                              if {@code recipientDevices} contains a {@code null} element
     */
    @Override
    public List<CallRekeyEnvelope> encryptRekeyFanout(Collection<Jid> recipientDevices, byte[] plaintext) {
        Objects.requireNonNull(recipientDevices, "recipientDevices cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        var devices = List.copyOf(recipientDevices);

        // A rekey continues each device's existing ratchet; ensure a session exists but do not force a
        // fresh one (forcing would send a pkmsg to a device that already has a session and break it).
        deviceService.ensureSessions(devices, false);

        var deviceIdentity = signedDeviceIdentity();
        var envelopes = new ArrayList<CallRekeyEnvelope>(devices.size());
        for (var deviceJid : devices) {
            try {
                var payload = encryption.encryptForDevice(deviceJid, plaintext);
                var identity = payload.isPreKeyMessage() ? deviceIdentity : null;
                envelopes.add(new CallRekeyEnvelope(deviceJid, payload.type(), payload.ciphertext(), identity));
            } catch (RuntimeException e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "rekey encryption failed for " + new LogRedactable.User(String.valueOf(deviceJid))
                                    + "; skipping this recipient",
                            e);
                }
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rekey fanout encrypted {0} of {1} devices", envelopes.size(), devices.size());
        }
        return envelopes;
    }

    /**
     * Decrypts an inbound call key envelope back to the thirty two byte raw call key.
     *
     * <p>Used for both the offer's per device {@code <enc>} and the group {@code <enc_rekey>}'s single
     * {@code <enc>}: both wrap the same {@code LinkedMessageContainer{Call{callKey}}} plaintext. The ciphertext
     * is Signal decrypted through {@link MessageService#processCall(Jid, MessageEncryptionType, byte[])}
     * (which strips the Signal PKCS#7 padding), the plaintext is decoded as a {@link LinkedMessageContainer},
     * and the key is read from its {@link CallOfferMessage#callKey() callKey}. This method never throws:
     * an empty result means "no end to end key recovered" so the caller can fall back to the hop by hop
     * key.
     *
     * @implNote This implementation decrypts through {@code processCall} under the
     * {@code SignalCryptoLocks} session lock, decodes the {@link LinkedMessageContainer}, and reads
     * {@code Call.callKey}. Any decrypt or decode failure is swallowed to an empty result; the engine
     * requires the recovered key to be at most {@value #CALL_KEY_LENGTH} bytes.
     *
     * @param senderJid  the device JID that authored the Signal envelope
     * @param encType    the Signal envelope variant from the {@code <enc>} {@code type} attribute
     * @param ciphertext the Signal ciphertext bytes from the {@code <enc>} content
     * @return the recovered call key, or an empty result when it could not be recovered
     * @throws NullPointerException if any argument is {@code null}
     */
    public Optional<byte[]> decryptCallKey(Jid senderJid, MessageEncryptionType encType, byte[] ciphertext) {
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(encType, "encType cannot be null");
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        try {
            var plaintext = messageService.processCall(senderJid, encType, ciphertext);
            var container = LinkedMessageContainerSpec.decode(plaintext);
            if (container.content() instanceof CallOfferMessage offer) {
                return offer.callKey();
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "call key decryption from " + new LogRedactable.User(String.valueOf(senderJid))
                                + " failed; treating as no end-to-end key",
                        e);
            }
            return Optional.empty();
        }
    }

    /**
     * Decrypts the call key from an inbound {@code <enc>} stanza, selecting its envelope variant and
     * ciphertext from the stanza's attributes and content.
     *
     * <p>This is the stanza level convenience over
     * {@link #decryptCallKey(Jid, MessageEncryptionType, byte[])}: it reads the {@code type} attribute
     * and the binary content of a single {@code <enc>} element (the bare {@code <enc>} a callee receives
     * directly under {@code <offer>}, or the {@code <enc>} child of an {@code <enc_rekey>}). An
     * {@code <enc>} with a missing or unparseable {@code type}, missing content, or a failed decrypt
     * yields an empty result without throwing.
     *
     * @param encStanza   the {@code <enc>} stanza carrying the Signal envelope
     * @param senderJid the device JID that authored the envelope, used as the Signal decryption sender
     * @return the recovered call key, or an empty result when it could not be recovered
     * @throws NullPointerException if {@code encStanza} or {@code senderJid} is {@code null}
     */
    public Optional<byte[]> decryptCallKey(Stanza encStanza, Jid senderJid) {
        Objects.requireNonNull(encStanza, "encStanza cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        var typeAttr = encStanza.getAttributeAsString(TYPE_ATTRIBUTE).orElse(null);
        if (typeAttr == null) {
            return Optional.empty();
        }
        MessageEncryptionType encType;
        try {
            encType = MessageEncryptionType.fromProtocolValue(typeAttr);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        var ciphertext = encStanza.toContentBytes().orElse(null);
        if (ciphertext == null) {
            return Optional.empty();
        }
        return decryptCallKey(senderJid, encType, ciphertext);
    }

    /**
     * Decrypts the call key from a single offer fanout slot.
     *
     * <p>This is the {@link CallKeyDistribution} level convenience over
     * {@link #decryptCallKey(Jid, MessageEncryptionType, byte[])}. A
     * {@linkplain CallKeyDistribution#isEncrypted() bare} slot, a slot whose {@code type} is absent or
     * unparseable, or a failed decrypt yields an empty result. The slot's
     * {@link CallKeyDistribution#deviceJid() device JID} is the recipient address, so the decryption
     * sender is supplied separately by the caller from the outer call stanza's {@code from}.
     *
     * @param slot      the offer fanout slot to decrypt
     * @param senderJid the device JID that authored the envelope, used as the Signal decryption sender
     * @return the recovered call key, or an empty result when it could not be recovered
     * @throws NullPointerException if {@code slot} or {@code senderJid} is {@code null}
     */
    @Override
    public Optional<byte[]> decryptCallKey(CallKeyDistribution slot, Jid senderJid) {
        Objects.requireNonNull(slot, "slot cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        if (!slot.isEncrypted()) {
            return Optional.empty();
        }
        var typeAttr = slot.typeValue().orElse(null);
        if (typeAttr == null) {
            return Optional.empty();
        }
        MessageEncryptionType encType;
        try {
            encType = MessageEncryptionType.fromProtocolValue(typeAttr);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        var ciphertext = slot.ciphertextBytes().orElse(null);
        if (ciphertext == null) {
            return Optional.empty();
        }
        return decryptCallKey(senderJid, encType, ciphertext);
    }

    /**
     * Returns the local ADV signed device identity bytes, or {@code null} when none is stored.
     *
     * <p>A {@code pkmsg} call key envelope bootstraps a new Signal session, so the recipient must learn
     * the sender's identity: the offer (when any device's {@code <enc>} is {@code pkmsg}) and every
     * {@code pkmsg} rekey carry this {@code <device identity>} block. The caller attaches it to the
     * stanza; this service exposes it so the offer and rekey builders share one source.
     *
     * @return the encoded ADV signed device identity, or {@code null} when none is stored
     */
    @Override
    public byte[] signedDeviceIdentity() {
        return store.signalStore().signedDeviceIdentity()
                .map(ADVSignedDeviceIdentitySpec::encode)
                .orElse(null);
    }

    /**
     * Maps a Signal encrypted payload to its offer fanout slot.
     *
     * <p>A keyed payload becomes an {@linkplain CallKeyDistribution#encrypted(Jid, int, String, int, byte[])
     * encrypted} slot carrying the Signal ciphertext version, the envelope type wire string, and the
     * {@value #ENC_COUNT} retry count; a bare destination marker becomes a
     * {@linkplain CallKeyDistribution#bare(Jid) bare} slot.
     *
     * @param payload the per device Signal encrypted payload
     * @return the matching fanout slot
     */
    private static CallKeyDistribution toFanoutSlot(MessageEncryptedPayload payload) {
        if (payload.ciphertext() == null || payload.type() == null) {
            return CallKeyDistribution.bare(payload.recipientJid());
        }
        return CallKeyDistribution.encrypted(
                payload.recipientJid(),
                MessageEncryption.CIPHERTEXT_VERSION,
                payload.type().protocolValue(),
                ENC_COUNT,
                payload.ciphertext());
    }
}
