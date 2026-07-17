package com.github.auties00.cobalt.message.receive.crypto;

import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.cobalt.message.send.bot.BotMessageSecret;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.SecretMessageContainerSpec;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.exception.*;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.protocol.SignalMessage;
import com.github.auties00.libsignal.protocol.SignalPreKeyMessage;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;
import it.auties.protobuf.exception.ProtobufDeserializationException;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Decrypts every flavour of incoming end-to-end encrypted WhatsApp payload behind a
 * single service.
 *
 * <p>The four entry points cover the four encryption types that can appear in a
 * stanza's {@code <enc>} children:
 * <ul>
 *   <li>{@link #decryptFromDevice(byte[], Jid, MessageEncryptionType)} for
 *       {@link MessageEncryptionType#PKMSG} and {@link MessageEncryptionType#MSG}
 *       through the Signal session cipher.</li>
 *   <li>{@link #decryptFromGroup(byte[], Jid, Jid)} for
 *       {@link MessageEncryptionType#SKMSG} through the Signal group cipher.</li>
 *   <li>{@link #decryptBotMessage(byte[], byte[], String, Jid, Jid)} for
 *       {@link MessageEncryptionType#MSMSG}, an AES-GCM scheme keyed via HKDF over the
 *       target message's secret.</li>
 *   <li>{@link #processSenderKeyDistribution(Jid, Jid, byte[])} for storing a received
 *       sender-key distribution so future {@link MessageEncryptionType#SKMSG} payloads
 *       can be decrypted.</li>
 * </ul>
 *
 * <p>The service is consumed by {@code ChatMessageReceiver}
 * as part of the inbound message pipeline.
 *
 * @implNote
 * This implementation maps libsignal's exception hierarchy onto the sealed
 * {@link WhatsAppMessageException.Receive} subtypes so the upstream pipeline can pick
 * the right receipt (delivery, retry, NACK) by branching on a strongly-typed
 * exception; WhatsApp Web folds the same Signal errors into a single
 * {@code SignalDecryptionError} and re-discriminates them via string-match inside
 * {@code WAWebSendRetryReceiptJob.getRetryReasonFromError}.
 */
@WhatsAppWebModule(moduleName = "WAWebMsgProcessingDecryptEnc")
@WhatsAppWebModule(moduleName = "WAWebSignalCipherApi")
@WhatsAppWebModule(moduleName = "WAWebBotMessageSecret")
@WhatsAppWebModule(moduleName = "WAWebCryptoLibrary")
@WhatsAppWebModule(moduleName = "WASignalGroupCipher")
public final class MessageDecryption {
    /**
     * The logger for {@link MessageDecryption}.
     */
    private static final System.Logger LOGGER = Log.get(MessageDecryption.class);

    /**
     * Holds the smallest valid PKCS#7 padding length used by the Signal protocol
     * payload format.
     */
    private static final int MIN_PADDING = 1;

    /**
     * Holds the largest valid PKCS#7 padding length used by the Signal protocol
     * payload format.
     */
    private static final int MAX_PADDING = 16;

    /**
     * Holds the HKDF-derived AES-GCM key length, in bytes, used for the
     * {@link MessageEncryptionType#MSMSG} bot-message scheme.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int HKDF_KEY_SIZE = 32;

    /**
     * Holds the AES-GCM authentication tag length, in bits, used for the
     * {@link MessageEncryptionType#MSMSG} bot-message scheme.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int AES_GCM_TAG_BITS = 128;

    /**
     * Holds the KDF algorithm identifier passed to {@link KDF#getInstance(String)} for
     * {@link MessageEncryptionType#MSMSG} key derivation.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    /**
     * Holds the cipher transformation identifier passed to
     * {@link Cipher#getInstance(String)} for {@link MessageEncryptionType#MSMSG}
     * decryption.
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";

    /**
     * Holds the central session store used for Signal-session and sender-key existence
     * checks and for resolving bot-message metadata.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the Signal session cipher used by
     * {@link #decryptFromDevice(byte[], Jid, MessageEncryptionType)}.
     */
    private final SignalSessionCipher sessionCipher;

    /**
     * Holds the Signal group cipher used by {@link #decryptFromGroup(byte[], Jid, Jid)}
     * and {@link #processSenderKeyDistribution(Jid, Jid, SignalSenderKeyDistributionMessage)}.
     */
    private final SignalGroupCipher groupCipher;

    /**
     * Holds the shared lock registry that serialises the non-atomic Signal session and sender-key ratchets so that
     * inbound decryptions and the outbound {@link MessageEncryption} encryptions of the same device session or
     * sender-key chain cannot interleave.
     */
    private final SignalCryptoLocks cryptoLocks;

    /**
     * Constructs the decryption service from its collaborators.
     *
     * @param store         the central session store used for sender-key and Signal
     *                      session lookups
     * @param sessionCipher the libsignal session cipher used for
     *                      {@link MessageEncryptionType#PKMSG} and
     *                      {@link MessageEncryptionType#MSG} decryption
     * @param groupCipher   the libsignal group cipher used for
     *                      {@link MessageEncryptionType#SKMSG} decryption and
     *                      sender-key import
     * @param cryptoLocks   the lock registry shared with {@link MessageEncryption} that serialises concurrent
     *                      session and sender-key ratchets
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptEnc", exports = "decryptEnc",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public MessageDecryption(
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
     * Decrypts a per-device Signal payload ({@link MessageEncryptionType#PKMSG} or
     * {@link MessageEncryptionType#MSG}).
     *
     * <p>The caller dispatches between {@link MessageEncryptionType#PKMSG} (new
     * session) and {@link MessageEncryptionType#MSG} (existing session) based on the
     * {@code <enc>} stanza's {@code type} attribute. The returned plaintext has the
     * Signal-protocol PKCS#7 padding already stripped by {@link #removePadding(byte[])}.
     *
     * @implNote
     * This implementation serialises the decrypt cycle for one device session through
     * {@link SignalCryptoLocks#withSession}, shared with the outbound {@link MessageEncryption}, so an inbound decrypt
     * and an outbound encrypt for the same session never ratchet it concurrently; WhatsApp Web never races them since
     * its JavaScript runs single-threaded. It maps the libsignal exception hierarchy onto the sealed
     * {@link WhatsAppMessageException.Receive} subtypes so receipt selection can branch
     * on a strongly-typed exception rather than a string-match against the underlying
     * libsignal message; {@link MessageEncryptionType#SKMSG} and
     * {@link MessageEncryptionType#MSMSG} throw {@link IllegalArgumentException} as a
     * misuse signal because the caller must route those to
     * {@link #decryptFromGroup(byte[], Jid, Jid)} and
     * {@link #decryptBotMessage(byte[], byte[], String, Jid, Jid)}.
     *
     * @param ciphertext     the encrypted message bytes
     * @param senderJid      the sender's device JID
     * @param encryptionType the encryption type ({@link MessageEncryptionType#PKMSG} or
     *                       {@link MessageEncryptionType#MSG})
     * @return the decrypted plaintext bytes with padding stripped
     * @throws WhatsAppMessageException.Receive if decryption fails
     * @throws IllegalArgumentException         if {@code encryptionType} is
     *                                          {@link MessageEncryptionType#SKMSG} or
     *                                          {@link MessageEncryptionType#MSMSG}
     * @throws NullPointerException             if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptEnc", exports = "decryptEnc",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSignalCipherApi", exports = "decryptSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "decryptSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public byte[] decryptFromDevice(byte[] ciphertext, Jid senderJid, MessageEncryptionType encryptionType) {
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(encryptionType, "encryptionType cannot be null");

        var address = senderJid.toSignalAddress();
        return cryptoLocks.withSession(address, () -> switch (encryptionType) {
            case PKMSG -> {
                try {
                    var message = SignalPreKeyMessage.ofSerialized(ciphertext);
                    var paddedPlaintext = sessionCipher.decrypt(address, message);
                    yield removePadding(paddedPlaintext);
                } catch (SignalException e) {
                    throw mapSessionError(e, senderJid, "PreKeyMessage");
                } catch (SecurityException e) {
                    throw new WhatsAppMessageException.Receive.InvalidMessage(
                            "Security verification failed for message from: " + senderJid, e);
                }
            }
            case MSG -> {
                try {
                    var message = SignalMessage.ofSerialized(ciphertext);
                    var paddedPlaintext = sessionCipher.decrypt(address, message);
                    yield removePadding(paddedPlaintext);
                } catch (SignalException e) {
                    throw mapSessionError(e, senderJid, "SignalMessage");
                } catch (SecurityException e) {
                    throw new WhatsAppMessageException.Receive.InvalidMessage(
                            "Security verification failed for message from: " + senderJid, e);
                }
            }
            case SKMSG -> throw new IllegalArgumentException("Use decryptFromGroup for SKMSG encryption type");
            case MSMSG -> throw new IllegalArgumentException("Use decryptBotMessage for MSMSG encryption type");
        });
    }

    /**
     * Maps a libsignal {@link SignalException} raised by the per-device
     * {@link SignalSessionCipher} onto the matching sealed
     * {@link WhatsAppMessageException.Receive} subtype.
     *
     * <p>The {@code kind} label distinguishes the two callers in the resulting
     * message text ({@code "PreKeyMessage"} for a {@code pkmsg},
     * {@code "SignalMessage"} for a {@code msg}). The mapping is:
     * <ul>
     *   <li>{@link SignalMalformedMessageException} (raised by
     *       {@code ofSerialized}) becomes
     *       {@link WhatsAppMessageException.Receive.InvalidMessage}.</li>
     *   <li>{@link SignalMissingSessionException} and
     *       {@link SignalUninitializedSessionException} become
     *       {@link WhatsAppMessageException.Receive.NoSession}.</li>
     *   <li>{@link SignalUntrustedIdentityException} and
     *       {@link SignalInvalidSignatureException} become
     *       {@link WhatsAppMessageException.Receive.InvalidSignature}.</li>
     *   <li>{@link SignalMissingPreKeyException} becomes
     *       {@link WhatsAppMessageException.Receive.InvalidOneTimeKey} and
     *       {@link SignalMissingSignedPreKeyException} becomes
     *       {@link WhatsAppMessageException.Receive.InvalidSignedPreKey}, both
     *       carrying the offending key id; these are the
     *       session-establishment failures a {@code pkmsg} hits when the
     *       referenced prekey is no longer held locally.</li>
     *   <li>{@link SignalDuplicateMessageException} becomes
     *       {@link WhatsAppMessageException.Receive.DuplicateMessage}.</li>
     *   <li>{@link SignalMissingReceiverChainException} and every other
     *       {@link SignalException} (including {@link SignalDecryptException})
     *       become {@link WhatsAppMessageException.Receive.Unknown}.</li>
     * </ul>
     * Every mapped subtype except {@link WhatsAppMessageException.Receive.DuplicateMessage}
     * reports {@link WhatsAppMessageException.Receive#shouldSendRetryReceipt()}
     * as {@code true}, so a missing prekey now drives a retry receipt rather
     * than a terminal NACK.
     *
     * @implNote
     * This implementation pattern-matches the concrete libsignal subtype rather
     * than the message text the previous build had to scrape, because libsignal
     * now exposes a dedicated exception per failure. The {@code default} arm
     * folds any unmapped {@link SignalException} onto
     * {@link WhatsAppMessageException.Receive.Unknown}, matching WhatsApp Web's
     * {@code WAWebSendRetryReceiptJob.getRetryReasonFromError} catch-all.
     *
     * @param e         the libsignal failure to translate
     * @param senderJid the sender's device JID
     * @param kind      the human-readable message kind for the detail text
     * @return the receive exception to throw
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "decryptSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private WhatsAppMessageException.Receive mapSessionError(SignalException e, Jid senderJid, String kind) {
        return switch (e) {
            case SignalMalformedMessageException malformed ->
                    new WhatsAppMessageException.Receive.InvalidMessage(
                            "Invalid " + kind + " format from: " + senderJid, malformed);
            case SignalMissingSessionException missing ->
                    new WhatsAppMessageException.Receive.NoSession(
                            "No session for " + kind + " from: " + senderJid, false, missing);
            case SignalUninitializedSessionException uninitialized ->
                    new WhatsAppMessageException.Receive.NoSession(
                            "Session not initialized for " + kind + " from: " + senderJid, false, uninitialized);
            case SignalUntrustedIdentityException identity ->
                    new WhatsAppMessageException.Receive.InvalidSignature(
                            "Identity key changed for: " + senderJid, identity);
            case SignalInvalidSignatureException signature ->
                    new WhatsAppMessageException.Receive.InvalidSignature(
                            "Signature verification failed for " + kind + " from: " + senderJid, signature);
            case SignalMissingPreKeyException preKey ->
                    new WhatsAppMessageException.Receive.InvalidOneTimeKey(
                            "One-time prekey " + preKey.id() + " missing for " + kind + " from: " + senderJid, preKey);
            case SignalMissingSignedPreKeyException signedPreKey ->
                    new WhatsAppMessageException.Receive.InvalidSignedPreKey(
                            "Signed prekey " + signedPreKey.id() + " missing for " + kind + " from: " + senderJid, signedPreKey);
            case SignalDuplicateMessageException duplicate ->
                    new WhatsAppMessageException.Receive.DuplicateMessage(
                            "Duplicate " + kind + " from: " + senderJid, duplicate);
            case SignalMissingReceiverChainException chain ->
                    new WhatsAppMessageException.Receive.Unknown(
                            "No matching receiver chain for " + kind + " from: " + senderJid, chain);
            default ->
                    new WhatsAppMessageException.Receive.Unknown(
                            "Decryption failed for " + kind + " from: " + senderJid, e);
        };
    }

    /**
     * Maps a libsignal {@link SignalException} raised by the
     * {@link SignalGroupCipher} onto the matching sealed
     * {@link WhatsAppMessageException.Receive} subtype.
     *
     * <p>The mapping is:
     * <ul>
     *   <li>{@link SignalMissingSenderKeyException} becomes
     *       {@link WhatsAppMessageException.Receive.NoSenderKey}.</li>
     *   <li>{@link SignalMissingSenderKeyStateException} becomes
     *       {@link WhatsAppMessageException.Receive.InvalidSenderKey}, carrying
     *       the unresolved sender-key state id.</li>
     *   <li>{@link SignalDuplicateMessageException} becomes
     *       {@link WhatsAppMessageException.Receive.DuplicateMessage}.</li>
     *   <li>{@link SignalMalformedMessageException} (raised when parsing the
     *       {@code SenderKeyMessage}) becomes
     *       {@link WhatsAppMessageException.Receive.InvalidMessage}.</li>
     *   <li>every other {@link SignalException} (including
     *       {@link SignalDecryptException}) becomes
     *       {@link WhatsAppMessageException.Receive.Unknown}.</li>
     * </ul>
     *
     * @implNote
     * This implementation pattern-matches the concrete libsignal subtype; the
     * group cipher now raises a dedicated {@link SignalDuplicateMessageException}
     * for an old-counter replay instead of the message-text marker the previous
     * build matched.
     *
     * @param e         the libsignal failure to translate
     * @param groupJid  the group, community, or broadcast JID
     * @param senderJid the sender's device JID
     * @return the receive exception to throw
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "decryptGroupSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private WhatsAppMessageException.Receive mapGroupError(SignalException e, Jid groupJid, Jid senderJid) {
        return switch (e) {
            case SignalMissingSenderKeyException missing ->
                    new WhatsAppMessageException.Receive.NoSenderKey(
                            "No sender key exists for group: " + groupJid + " sender: " + senderJid, missing);
            case SignalMissingSenderKeyStateException state ->
                    new WhatsAppMessageException.Receive.InvalidSenderKey(
                            "Sender key state not found for ID " + state.id().orElse(-1) +
                                    " in group: " + groupJid + " sender: " + senderJid, state);
            case SignalDuplicateMessageException duplicate ->
                    new WhatsAppMessageException.Receive.DuplicateMessage(
                            "Duplicate group message from: " + senderJid + " in group: " + groupJid, duplicate);
            case SignalMalformedMessageException malformed ->
                    new WhatsAppMessageException.Receive.InvalidMessage(
                            "Invalid SenderKeyMessage format from: " + senderJid + " in group: " + groupJid, malformed);
            default ->
                    new WhatsAppMessageException.Receive.Unknown(
                            "Group decryption failed for message from: " + senderJid + " in group: " + groupJid, e);
        };
    }

    /**
     * Decrypts a group, community, or broadcast sender-key payload
     * ({@link MessageEncryptionType#SKMSG}).
     *
     * <p>The returned plaintext has Signal-protocol PKCS#7 padding stripped by
     * {@link #removePadding(byte[])}.
     *
     * @implNote
     * This implementation serialises the decrypt cycle for one sender-key chain through
     * {@link SignalCryptoLocks#withSenderKey}, shared with the
     * {@link #processSenderKeyDistribution(Jid, Jid, SignalSenderKeyDistributionMessage)} import and the outbound
     * {@link MessageEncryption}, so concurrent decrypts and a distribution import never ratchet the same chain at once;
     * WhatsApp Web never races them since its JavaScript runs single-threaded. It folds every libsignal
     * {@link SignalException} subtype onto the sealed {@link WhatsAppMessageException.Receive} hierarchy through
     * {@link #mapGroupError(SignalException, Jid, Jid)}, and additionally maps a {@link SecurityException} from the
     * sender-key signature check onto {@link WhatsAppMessageException.Receive.InvalidSenderKey}.
     *
     * @param ciphertext the encrypted message bytes
     * @param groupJid   the group, community, or broadcast JID
     * @param senderJid  the sender's device JID
     * @return the decrypted plaintext bytes with padding stripped
     * @throws WhatsAppMessageException.Receive if decryption fails
     * @throws NullPointerException             if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMsgProcessingDecryptEnc", exports = "decryptEnc",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSignalCipherApi", exports = "decryptGroupSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "decryptGroupSignalProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "decryptSenderKeyMsgFromSession",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "deserializeSenderKeyMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public byte[] decryptFromGroup(byte[] ciphertext, Jid groupJid, Jid senderJid) {
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");

        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);

        return cryptoLocks.withSenderKey(senderKeyName, () -> {
            try {
                var paddedPlaintext = groupCipher.decrypt(senderKeyName, ciphertext);
                return removePadding(paddedPlaintext);
            } catch (SignalException e) {
                throw mapGroupError(e, groupJid, senderJid);
            } catch (SecurityException e) {
                throw new WhatsAppMessageException.Receive.InvalidSenderKey(
                        "Sender key signature verification failed from: " + senderJid + " in group: " + groupJid, e);
            }
        });
    }

    /**
     * Decrypts a bot-message payload ({@link MessageEncryptionType#MSMSG}).
     *
     * <p>The caller resolves the {@code messageId} (preferring {@code botEditTargetId}
     * from the {@code <bot>} stanza child when present), looks up the
     * {@code messageSecret} from the target message, and resolves the sender JIDs from
     * the stanza's addressing attributes. The {@code ciphertext} is a serialised
     * {@code MessageSecretMessage} protobuf carrying {@code encIv} and
     * {@code encPayload} fields.
     *
     * @implNote
     * This implementation derives the AES-GCM key as
     * {@snippet :
     *     // info = messageId || targetSenderJid || botSenderJid (UTF-8 bytes)
     *     // aesKey = HKDF-SHA256(IKM = BotMessageSecret.derive(messageSecret),
     *     //                      salt = empty,
     *     //                      info = info,
     *     //                      L = 32)
     *     // aad = messageId || 0x00 || botSenderJid
     * }
     * mirroring WhatsApp Web's {@code WAWebBotMessageSecret.decryptMsmsgBotMessage}.
     * The {@code Bot Message} label that WhatsApp Web feeds into the inner HKDF over
     * the raw secret is handled inside {@link BotMessageSecret#derive(byte[])}, so this
     * method only sees the derived base secret.
     *
     * @param ciphertext      the {@link MessageEncryptionType#MSMSG} ciphertext (a
     *                        {@code MessageSecretMessage} protobuf)
     * @param messageSecret   the 32-byte secret from the target message
     * @param messageId       the message id used for key derivation and AAD
     * @param targetSenderJid the target message sender's user JID
     * @param botSenderJid    the bot sender's user JID
     * @return the decrypted inner plaintext
     * @throws WhatsAppMessageException.Receive if decryption fails
     * @throws NullPointerException             if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public byte[] decryptBotMessage(
            byte[] ciphertext,
            byte[] messageSecret,
            String messageId,
            Jid targetSenderJid,
            Jid botSenderJid
    ) {
        Objects.requireNonNull(ciphertext, "ciphertext");
        Objects.requireNonNull(messageSecret, "messageSecret");
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(targetSenderJid, "targetSenderJid");
        Objects.requireNonNull(botSenderJid, "botSenderJid");

        try {
            var secretMessage = SecretMessageContainerSpec.decode(ciphertext);
            var encIv = secretMessage.encIv().orElseThrow(() ->
                    new WhatsAppMessageException.Receive.InvalidMessage(
                            "MSMSG missing encIv", null));
            var encPayload = secretMessage.encPayload().orElseThrow(() ->
                    new WhatsAppMessageException.Receive.InvalidMessage(
                            "MSMSG missing encPayload", null));

            var botSecret = BotMessageSecret.derive(messageSecret);
            var aesKey = deriveBotPerMessageKey(
                    messageId, targetSenderJid, botSenderJid, botSecret);
            var aad = buildBotAad(messageId, botSenderJid);

            var cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            var keySpec = new SecretKeySpec(aesKey, "AES");
            var gcmSpec = new GCMParameterSpec(AES_GCM_TAG_BITS, encIv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            cipher.updateAAD(aad);
            return cipher.doFinal(encPayload);
        } catch (WhatsAppMessageException.Receive e) {
            throw e;
        } catch (Exception e) {
            throw new WhatsAppMessageException.Receive.Unknown(
                    "MSMSG decryption failed", e);
        }
    }

    /**
     * Strips Signal-protocol PKCS#7 padding from a decrypted plaintext.
     *
     * <p>Applied by the per-device and group decryption branches before returning to
     * the caller, and not applied to {@link MessageEncryptionType#MSMSG} ciphertexts.
     *
     * @implNote
     * This implementation reads the last byte as the padding length, validates that it
     * falls in the {@code [MIN_PADDING, MAX_PADDING]} range, and returns a fresh array
     * containing only the unpadded prefix; mirrors WhatsApp Web's
     * {@code WACryptoPkcs7.unpadPkcs7}. WhatsApp Web's
     * {@code WAWebMsgProcessingDecryptApi.processDecryptedMessageProto} also skips
     * unpadding for the {@code Msmsg} ciphertext type, which is why
     * {@link #decryptBotMessage(byte[], byte[], String, Jid, Jid)} never calls this
     * method.
     *
     * @param paddedPlaintext the padded plaintext bytes
     * @return the plaintext with padding removed
     * @throws IllegalArgumentException if the padding length is missing or invalid
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgProcess", exports = "processDecryptedMessageProto",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WACryptoPkcs7", exports = "unpadPkcs7",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static byte[] removePadding(byte[] paddedPlaintext) {
        Objects.requireNonNull(paddedPlaintext, "paddedPlaintext cannot be null");

        if (paddedPlaintext.length == 0) {
            throw new IllegalArgumentException("Padded plaintext cannot be empty");
        }

        var paddingLength = paddedPlaintext[paddedPlaintext.length - 1] & 0xFF;

        if (paddingLength < MIN_PADDING || paddingLength > MAX_PADDING) {
            throw new IllegalArgumentException(
                    "Invalid padding length: " + paddingLength + " (expected " + MIN_PADDING + "-" + MAX_PADDING + ")"
            );
        }

        if (paddingLength > paddedPlaintext.length) {
            throw new IllegalArgumentException(
                    "Padding length " + paddingLength + " exceeds message length " + paddedPlaintext.length
            );
        }

        var originalLength = paddedPlaintext.length - paddingLength;
        return Arrays.copyOf(paddedPlaintext, originalLength);
    }

    /**
     * Stores a received sender-key distribution message so future
     * {@link MessageEncryptionType#SKMSG} payloads from the same sender in the same
     * group can be decrypted.
     *
     * <p>The caller verifies that the embedded group id matches the stanza chat JID
     * before invoking this method.
     *
     * @implNote This implementation imports the distribution under the {@link SignalCryptoLocks#withSenderKey} lock,
     * shared with {@link #decryptFromGroup(byte[], Jid, Jid)}, so the import cannot interleave with a concurrent decrypt
     * of the same sender-key chain.
     *
     * @param groupJid        the group JID
     * @param senderJid       the sender's device JID
     * @param distributionMsg the parsed Signal sender-key distribution message
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "processSenderKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "processSenderKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void processSenderKeyDistribution(Jid groupJid, Jid senderJid, SignalSenderKeyDistributionMessage distributionMsg) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(distributionMsg, "distributionMsg cannot be null");

        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);
        cryptoLocks.withSenderKey(senderKeyName, () -> {
            groupCipher.process(senderKeyName, distributionMsg);
        });
        if (Log.TRACE) LOGGER.log(Level.TRACE,
                "installed sender key distribution for group {0} from {1}", groupJid, senderJid);
    }

    /**
     * Stores a received sender-key distribution from raw protobuf bytes, parsing before
     * delegating to
     * {@link #processSenderKeyDistribution(Jid, Jid, SignalSenderKeyDistributionMessage)}.
     *
     * <p>This overload accepts the raw distribution-message bytes extracted from the
     * decrypted {@code LinkedMessageContainer}.
     *
     * @param groupJid         the group JID
     * @param senderJid        the sender's device JID
     * @param distributionData the raw distribution-message bytes
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "processSenderKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "processSenderKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASignalGroupCipher", exports = "deserializeSenderKeyMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void processSenderKeyDistribution(Jid groupJid, Jid senderJid, byte[] distributionData) {
        Objects.requireNonNull(distributionData, "distributionData cannot be null");

        var distributionMsg = SignalSenderKeyDistributionMessage.ofSerialized(distributionData);
        processSenderKeyDistribution(groupJid, senderJid, distributionMsg);
    }

    /**
     * Returns whether a Signal session exists for the given device.
     *
     * <p>Serves as a defensive lookup before invoking
     * {@link #decryptFromDevice(byte[], Jid, MessageEncryptionType)} when the caller
     * needs to know whether a {@link MessageEncryptionType#PKMSG}-style session
     * installation is still pending.
     *
     * @param deviceJid the device JID to check
     * @return {@code true} when a Signal session record is present
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "getRemoteRegId",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean hasSessionWith(Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");

        var address = deviceJid.toSignalAddress();
        return store.signalStore().findSessionByAddress(address).isPresent();
    }

    /**
     * Returns whether a sender-key record exists for the given group and sender.
     *
     * <p>Lets a caller gate a send or receive decision on the existence of the
     * sender-key state before invoking the group cipher.
     *
     * @param groupJid  the group JID
     * @param senderJid the sender's device JID
     * @return {@code true} when a sender-key record is present
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebCryptoLibrary", exports = "getGroupSenderKeyInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean hasSenderKey(Jid groupJid, Jid senderJid) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");

        var senderKeyName = SenderKeyNameFactory.create(groupJid, senderJid);
        return store.signalStore().findSenderKeyByName(senderKeyName).isPresent();
    }

    /**
     * Extracts the sender's identity public key from a
     * {@link MessageEncryptionType#PKMSG} ciphertext.
     *
     * <p>Used by Auxiliary Device Validation (ADV) so the receiver can verify the
     * companion device's identity signature before attempting decryption.
     *
     * @implNote
     * This implementation parses the {@link MessageEncryptionType#PKMSG} envelope just
     * enough to surface the identity-key field; the inner message is not decrypted
     * here. A {@link ProtobufDeserializationException} or {@link IllegalArgumentException}
     * surfaces as an empty {@link Optional} so the caller decides whether to treat the
     * failure as a fatal ADV mismatch or to retry.
     *
     * @param ciphertext the {@link MessageEncryptionType#PKMSG} ciphertext bytes
     * @return an {@link Optional} wrapping the 32-byte identity-key encoded point
     */
    @WhatsAppWebExport(moduleName = "WAWebSignalUtilsApi", exports = "extractIdentityKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<byte[]> extractIdentityKeyFromPkmsg(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) {
            return Optional.empty();
        }

        try {
            var message = SignalPreKeyMessage.ofSerialized(ciphertext);
            var identityKey = message.identityKey();
            if (identityKey == null) {
                return Optional.empty();
            }
            return Optional.of(identityKey.toEncodedPoint());
        } catch (ProtobufDeserializationException | IllegalArgumentException e) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "failed to extract identity key from pkmsg: {0}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Derives the per-message AES-GCM key for {@link MessageEncryptionType#MSMSG}
     * decryption.
     *
     * @implNote
     * This implementation performs HKDF-SHA256 with an empty (default) salt over the
     * bot secret and an {@code info} string built as
     * {@code messageId || targetSenderJid || botSenderJid} (UTF-8 bytes, no
     * delimiters). The output length is {@value #HKDF_KEY_SIZE} bytes. Mirrors WhatsApp
     * Web's {@code v} helper inside {@code WAWebBotMessageSecret}.
     *
     * @param messageId       the message id (or bot edit-target id)
     * @param targetSenderJid the target message sender's user JID
     * @param botSenderJid    the bot sender's user JID
     * @param botSecret       the base bot secret derived from {@code messageSecret}
     * @return the 32-byte AES-GCM key
     * @throws GeneralSecurityException if the HKDF provider is unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private byte[] deriveBotPerMessageKey(
            String messageId,
            Jid targetSenderJid,
            Jid botSenderJid,
            byte[] botSecret
    ) throws GeneralSecurityException {
        var idBytes = messageId.getBytes(StandardCharsets.UTF_8);
        var targetBytes = targetSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var botBytes = botSenderJid.toString().getBytes(StandardCharsets.UTF_8);

        var info = new byte[idBytes.length + targetBytes.length + botBytes.length];
        System.arraycopy(idBytes, 0, info, 0, idBytes.length);
        System.arraycopy(targetBytes, 0, info, idBytes.length, targetBytes.length);
        System.arraycopy(botBytes, 0, info, idBytes.length + targetBytes.length, botBytes.length);

        var kdf = KDF.getInstance(HKDF_ALGORITHM);
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(botSecret)
                .thenExpand(info, HKDF_KEY_SIZE);
        return kdf.deriveData(params);
    }

    /**
     * Builds the additional authenticated data for the {@link MessageEncryptionType#MSMSG}
     * AES-GCM decryption.
     *
     * @implNote
     * This implementation builds the AAD as {@code messageId || 0x00 || botSenderJid}
     * with bytes encoded in UTF-8; mirrors WhatsApp Web's
     * {@code WAWebBotMessageSecret.decryptMsmsgBotMessage} call site that passes
     * {@code y+"\0"+_} to {@code WACryptoAesGcm.gcmDecrypt}.
     *
     * @param messageId    the message id
     * @param botSenderJid the bot sender's user JID
     * @return the AAD bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "decryptMsmsgBotMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private byte[] buildBotAad(String messageId, Jid botSenderJid) {
        var idBytes = messageId.getBytes(StandardCharsets.UTF_8);
        var botBytes = botSenderJid.toString().getBytes(StandardCharsets.UTF_8);
        var aad = new byte[idBytes.length + 1 + botBytes.length];
        System.arraycopy(idBytes, 0, aad, 0, idBytes.length);
        aad[idBytes.length] = 0x00;
        System.arraycopy(botBytes, 0, aad, idBytes.length + 1, botBytes.length);
        return aad;
    }
}
