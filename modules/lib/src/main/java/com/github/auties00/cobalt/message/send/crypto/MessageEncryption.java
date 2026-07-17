package com.github.auties00.cobalt.message.send.crypto;

import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryption;
import com.github.auties00.cobalt.message.receive.crypto.SenderKeyNameFactory;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Encrypts outbound message protobufs into Signal envelopes for per-device fanout or group sender-key delivery.
 * <p>
 * This service is the sending counterpart of {@link MessageDecryption}: every outgoing
 * {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer} that leaves the client passes through
 * {@link #encryptForDevice(Jid, byte[])} for 1:1 and companion fanout, or through {@link #encryptForGroup(Jid, Jid, byte[])}
 * for SKMSG group delivery. It is held by the stanza-build pipeline as an injected service; embedders that speak Signal
 * directly do not normally call it.
 */
@WhatsAppWebModule(moduleName = "WAWebEncryptMsgProtobuf")
@WhatsAppWebModule(moduleName = "WAWebBackendJobsCommon")
@WhatsAppWebModule(moduleName = "WAWebSignalCipherApi")
@WhatsAppWebModule(moduleName = "WAWebSignalSessionApi")
@WhatsAppWebModule(moduleName = "WAWebCryptoLibrary")
@WhatsAppWebModule(moduleName = "WASignalGroupCipher")
public final class MessageEncryption {
    /**
     * The logger for {@link MessageEncryption}.
     */
    private static final System.Logger LOGGER = Log.get(MessageEncryption.class);

    /**
     * Holds the ciphertext format version stamped on every outgoing {@code <enc>} stanza.
     * <p>
     * The fanout-stanza writer copies this value into the {@code v} attribute of every encrypted child element. It is
     * bumped only in lockstep with the WhatsApp Web bundle, so callers treat it as a read-only wire constant.
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobsCommon", exports = "CIPHERTEXT_VERSION",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int CIPHERTEXT_VERSION = 2;

    /**
     * Holds the store consulted for Signal session and sender-key lookups.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the per-device Signal session cipher used by {@link #encryptForDevice(Jid, byte[])}.
     */
    private final SignalSessionCipher sessionCipher;

    /**
     * Holds the sender-key group cipher used by {@link #encryptForGroup(Jid, Jid, byte[])} and
     * {@link #createSenderKeyDistributionMessage(Jid, Jid)}.
     */
    private final SignalGroupCipher groupCipher;

    /**
     * Holds the shared lock registry that serialises the non-atomic Signal session and sender-key ratchets so that
     * concurrent encryptions and the inbound {@link MessageDecryption} decryptions of the same device session or
     * sender-key chain cannot interleave.
     */
    private final SignalCryptoLocks cryptoLocks;

    /**
     * Holds the minimum number of random padding bytes appended to a plaintext before encryption.
     */
    private static final int MIN_PADDING = 1;

    /**
     * Holds the maximum number of random padding bytes appended to a plaintext before encryption.
     */
    private static final int MAX_PADDING = 16;

    /**
     * Constructs an encryption service bound to the given Signal dependencies.
     * <p>
     * The same {@link LinkedWhatsAppStore} must be shared with the {@link SignalSessionCipher} and {@link SignalGroupCipher} so
     * that session lookups during encrypt-then-cleanup observe the same backing state.
     *
     * @param store         the store providing Signal protocol state
     * @param sessionCipher the cipher used for {@link #encryptForDevice(Jid, byte[])}
     * @param groupCipher   the cipher used for {@link #encryptForGroup(Jid, Jid, byte[])} and
     *                      {@link #createSenderKeyDistributionMessage(Jid, Jid)}
     * @param cryptoLocks   the lock registry shared with {@link MessageDecryption} that serialises concurrent
     *                      session and sender-key ratchets
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptMsgProtobuf", exports = {"encryptMsgProtobuf", "encryptMsgSenderKey"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    public MessageEncryption(
            LinkedWhatsAppStore store,
            SignalSessionCipher sessionCipher,
            SignalGroupCipher groupCipher,
            SignalCryptoLocks cryptoLocks
    ) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.sessionCipher = Objects.requireNonNull(sessionCipher, "sessionCipher cannot be null");
        this.groupCipher = Objects.requireNonNull(groupCipher, "groupCipher cannot be null");
        this.cryptoLocks = Objects.requireNonNull(cryptoLocks, "cryptoLocks cannot be null");
    }

    /**
     * Encrypts the given plaintext for a specific recipient device.
     * <p>
     * The {@code plaintext} input is the protobuf-encoded {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer}.
     * The result is a {@link MessageEncryptionType#PKMSG} payload when the Signal session is freshly established and a
     * {@link MessageEncryptionType#MSG} payload once the recipient has decrypted at least one PKMSG. A thrown
     * {@link WhatsAppMessageException.Send.Unknown} aborts the per-device branch for that recipient; the caller decides
     * whether the recipient is critical (a primary device, in which case the whole send fails) or skippable (a companion).
     *
     * @implNote This implementation serialises the encrypt cycle for one recipient device through
     * {@link SignalCryptoLocks#withSession}, keyed by the recipient {@link SignalProtocolAddress} and shared with the
     * inbound {@link MessageDecryption}, because the Signal session ratchet is a non-atomic load-ratchet-store cycle;
     * WhatsApp Web never races it since its JavaScript runs single-threaded, whereas Cobalt encrypts and decrypts on
     * virtual threads. On encryption error it removes the failing Signal session via {@link LinkedWhatsAppSignalStore#removeSession}
     * so a later retry on the same address triggers a fresh PreKey exchange rather than re-using the stale session
     * record.
     *
     * @param recipientJid the recipient device {@link Jid}
     * @param plaintext    the protobuf-encoded plaintext bytes
     * @return the encrypted payload tagged with its envelope type
     * @throws NullPointerException                  if any argument is {@code null}
     * @throws WhatsAppMessageException.Send.Unknown if encryption fails
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptMsgProtobuf", exports = "encryptMsgProtobuf",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSignalCipherApi", exports = "encryptSignalProto",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "encryptSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public MessageEncryptedPayload encryptForDevice(Jid recipientJid, byte[] plaintext) {
        Objects.requireNonNull(recipientJid, "recipientJid cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");

        var paddedPlaintext = addPadding(plaintext);
        var address = recipientJid.toSignalAddress();

        return cryptoLocks.withSession(address, () -> {
            try {
                var ciphertextMessage = sessionCipher.encrypt(address, paddedPlaintext);
                var encryptionType = MessageEncryptionType.fromSignalCiphertext(ciphertextMessage);

                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "encrypted message for {0}, type={1}", recipientJid, encryptionType);
                }

                return new MessageEncryptedPayload(
                        encryptionType,
                        ciphertextMessage.toSerialized(),
                        recipientJid
                );
            } catch (Exception e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "encryptMsgProtobuf: encryption fail for " + new LogRedactable.User(String.valueOf(recipientJid)), e);
                }

                try {
                    store.signalStore().removeSession(address);
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "removed stale session for {0} after encryption failure", recipientJid);
                    }
                } catch (Exception cleanupError) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "session cleanup failed for " + new LogRedactable.User(String.valueOf(recipientJid)), cleanupError);
                    }
                }

                throw new WhatsAppMessageException.Send.Unknown(
                        "Failed to encrypt message for device: " + recipientJid, e
                );
            }
        });
    }

    /**
     * Encrypts the given plaintext for a group using sender-key encryption.
     * <p>
     * The resulting SKMSG ciphertext is delivered once to the group; every member who already holds the sender's
     * {@link SignalSenderKeyDistributionMessage} can decrypt it locally. Members without the distribution must receive it
     * first through {@link com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution#encrypt(Jid, byte[], java.util.Collection)}.
     * The returned payload always has a {@code null} {@link MessageEncryptedPayload#recipientJid()}.
     *
     * @implNote This implementation serialises the encrypt cycle for one sender-key chain through
     * {@link SignalCryptoLocks#withSenderKey}, keyed by the {@link SignalSenderKeyName} and shared with
     * {@link #createSenderKeyDistributionMessage(Jid, Jid)}, {@link #rotateSenderKey(Jid, Jid)}, and the inbound
     * {@link MessageDecryption}, so the check-then-create bootstrap and the chain ratchet cannot interleave across
     * virtual threads; WhatsApp Web never races them since its JavaScript runs single-threaded. It lazily bootstraps the
     * sender-key state via {@link SignalGroupCipher#create} when the store does not already hold one for this sender.
     *
     * @param groupJid  the group {@link Jid}
     * @param senderJid the sender device {@link Jid}, PN or LID depending on the group's addressing mode
     * @param plaintext the protobuf-encoded plaintext bytes
     * @return the SKMSG-typed encrypted payload
     * @throws NullPointerException                  if any argument is {@code null}
     * @throws WhatsAppMessageException.Send.Unknown if encryption fails
     */
    @WhatsAppWebExport(moduleName = "WAWebEncryptMsgProtobuf", exports = "encryptMsgSenderKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSignalCipherApi", exports = "encryptSenderKeyMsgSignalProto",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "encryptSenderKeyMsgSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "encryptSenderKeyMsgWithSession",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public MessageEncryptedPayload encryptForGroup(Jid groupJid, Jid senderJid, byte[] plaintext) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");

        var paddedPlaintext = addPadding(plaintext);
        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);

        return cryptoLocks.withSenderKey(senderKeyName, () -> {
            if (store.signalStore().findSenderKeyByName(senderKeyName).isEmpty()) {
                groupCipher.create(senderKeyName);
            }

            try {
                var ciphertextMessage = groupCipher.encrypt(senderKeyName, paddedPlaintext);

                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "encrypted group message for {0}, sender={1}", groupJid, senderJid);
                }

                return new MessageEncryptedPayload(
                        MessageEncryptionType.SKMSG,
                        ciphertextMessage.toSerialized(),
                        null
                );
            } catch (Exception e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "encryptMsgSenderKey: encryption fail for " + new LogRedactable.User(String.valueOf(groupJid)), e);
                }
                throw new WhatsAppMessageException.Send.Unknown(
                        "Failed to encrypt group message for group: " + groupJid, e
                );
            }
        });
    }

    /**
     * Appends {@value #MIN_PADDING} to {@value #MAX_PADDING} random padding bytes to the supplied plaintext.
     * <p>
     * Every padding byte carries the padding length value, so the recipient recovers the original length by reading the
     * last byte. Padding is applied before encryption to mask the plaintext length distribution.
     *
     * @implNote This implementation samples the padding length from the low four bits of a single random byte and adds
     * {@value #MIN_PADDING}, yielding the inclusive range {@code [1, 16]}.
     *
     * @param plaintext the unpadded plaintext bytes
     * @return the padded plaintext bytes
     * @throws NullPointerException if {@code plaintext} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "encodeAndPad",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSignalCommonUtils", exports = "writeRandomPadMax16",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WACryptoPkcs7", exports = "writeRandomPadMax16",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static byte[] addPadding(byte[] plaintext) {
        Objects.requireNonNull(plaintext, "plaintext cannot be null");

        var paddingLength = MIN_PADDING + (DataUtils.randomByteArray(1)[0] & 0x0F);

        var padded = new byte[plaintext.length + paddingLength];
        System.arraycopy(plaintext, 0, padded, 0, plaintext.length);

        for (var i = plaintext.length; i < padded.length; i++) {
            padded[i] = (byte) paddingLength;
        }

        return padded;
    }

    /**
     * Creates and returns the sender-key distribution message for the given group and sender pair.
     * <p>
     * The distribution message must be delivered, via per-device PKMSG or MSG encryption, to every group member that does
     * not yet hold the sender's key before they can decrypt any SKMSG produced by {@link #encryptForGroup(Jid, Jid, byte[])}.
     *
     * @implNote This implementation acquires the {@link SignalSenderKeyName} lock via
     * {@link SignalCryptoLocks#withSenderKey}, shared with {@link #encryptForGroup(Jid, Jid, byte[])}, so a concurrent
     * send cannot lazily create or ratchet the chain between this caller's create and its read of the distribution.
     *
     * @param groupJid  the group {@link Jid}
     * @param senderJid the sender device {@link Jid}
     * @return the sender-key distribution message
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalSessionApi", exports = "getGroupSenderKeyInfo",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "createSenderKeyDistributionProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public SignalSenderKeyDistributionMessage createSenderKeyDistributionMessage(Jid groupJid, Jid senderJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");

        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);
        return cryptoLocks.withSenderKey(senderKeyName, () -> groupCipher.create(senderKeyName));
    }

    /**
     * Returns the serialised sender-key distribution bytes for the given group and sender pair.
     * <p>
     * This is a convenience wrapper over {@link #createSenderKeyDistributionMessage(Jid, Jid)} that hands the
     * already-serialised proto straight to the per-device encryption path in
     * {@link com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution#encrypt(Jid, byte[], java.util.Collection)}.
     *
     * @param groupJid  the group {@link Jid}
     * @param senderJid the sender device {@link Jid}
     * @return the serialised sender-key distribution message bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebGetGroupKeyDistributionMsg", exports = "getKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public byte[] getSenderKeyBytes(Jid groupJid, Jid senderJid) {
        var distributionMessage = createSenderKeyDistributionMessage(groupJid, senderJid);
        return distributionMessage.toSerialized();
    }

    /**
     * Removes the sender key for the given group and sender pair, forcing it to be regenerated on the next send.
     * <p>
     * This is called after a participant change (a member removed, added, or rotated) invalidates the existing sender key,
     * so the next {@link #encryptForGroup(Jid, Jid, byte[])} call lazily creates a fresh distribution and re-fans it out
     * via {@link com.github.auties00.cobalt.message.send.senderkey.SenderKeyDistribution#encrypt(Jid, byte[], java.util.Collection)}.
     *
     * @implNote This implementation acquires the {@link SignalSenderKeyName} lock via
     * {@link SignalCryptoLocks#withSenderKey} so the removal cannot interleave with an in-flight
     * {@link #encryptForGroup(Jid, Jid, byte[])} on the same chain.
     *
     * @param groupJid  the group {@link Jid}
     * @param senderJid the sender device {@link Jid}
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalSessionApi", exports = "deleteGroupSenderKeyInfo",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void rotateSenderKey(Jid groupJid, Jid senderJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");

        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);
        cryptoLocks.withSenderKey(senderKeyName, () -> {
            store.signalStore().removeSenderKeys(senderKeyName);

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "rotated sender key for group {0}, sender {1}", groupJid, senderJid);
            }
        });
    }

    /**
     * Returns whether a Signal session already exists with the given device.
     * <p>
     * The fanout planner consults this to decide whether a per-device send must be preceded by a PreKey exchange (no
     * session) or can proceed directly (session present).
     *
     * @param deviceJid the device {@link Jid} to query
     * @return {@code true} when a Signal session exists with {@code deviceJid}
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalSessionApi", exports = "hasSignalSessions",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean hasSessionWith(Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        var address = deviceJid.toSignalAddress();
        return store.signalStore().findSessionByAddress(address).isPresent();
    }

    /**
     * Returns whether a sender key already exists for the given group and sender pair.
     * <p>
     * The group send planner consults this to skip the distribution-fanout step when the sender key is already
     * established for every recipient; the complementary {@link #rotateSenderKey(Jid, Jid)} clears this state when the
     * group membership changes.
     *
     * @param groupJid  the group {@link Jid}
     * @param senderJid the sender device {@link Jid}
     * @return {@code true} when a sender key is present for this pair
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean hasSenderKey(Jid groupJid, Jid senderJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);
        return store.signalStore().findSenderKeyByName(senderKeyName).isPresent();
    }

}
