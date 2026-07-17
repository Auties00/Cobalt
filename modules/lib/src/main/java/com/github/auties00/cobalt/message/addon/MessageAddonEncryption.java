package com.github.auties00.cobalt.message.addon;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Encrypts and decrypts the inner AES-GCM layer shared by every dual-encrypted
 * WhatsApp addon: poll votes, community and announcement group reactions,
 * encrypted comments, event responses, event edits, poll edits, poll
 * add-options, and message edits.
 *
 * <p>Addon messages live inside the outer Signal envelope but carry their own
 * inner ciphertext so the server can route the addon without correlating its
 * content with the parent message. The inner layer uses a 32-byte secret
 * derived via HKDF-SHA256 from the parent message's {@code messageSecret},
 * with the parent stanza id, original sender JID, addon sender JID, and
 * use-case label mixed into the info parameter. The result is an AES-256-GCM
 * ciphertext with a random 12-byte IV and a 128-bit authentication tag. The
 * {@link MessageAddonType#POLL_VOTE} and {@link MessageAddonType#EVENT_RESPONSE}
 * use cases additionally authenticate {@code stanzaId + "\0" + addonSender} as
 * AAD to prevent the server from rebinding a vote or RSVP from one user to
 * another.
 *
 * @implNote This implementation rides directly on the JDK's {@link KDF} and
 * {@link Cipher} APIs. WA Web's {@code WAWebAddonEncryption} additionally falls
 * back through LID and PN variants of the sender JID on decrypt failure (LID
 * first, then PN, then the original WID); Cobalt does not yet replay those
 * fallbacks, so a JID-form mismatch surfaces as a decrypt
 * {@link RuntimeException}.
 */
@WhatsAppWebModule(moduleName = "WAWebAddonEncryption")
@WhatsAppWebModule(moduleName = "WAUseCaseSecret")
public final class MessageAddonEncryption {
    /**
     * The logger for {@link MessageAddonEncryption}.
     */
    private static final System.Logger LOGGER = Log.get(MessageAddonEncryption.class);

    /**
     * Holds the size of the AES-GCM initialisation vector, in bytes.
     */
    private static final int AES_GCM_IV_SIZE = 12;

    /**
     * Holds the size of the AES-GCM authentication tag, in bits.
     */
    private static final int AES_GCM_TAG_SIZE = 128;

    /**
     * Holds the output size of the HKDF-derived use-case secret, in bytes.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "createUseCaseSecret",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int HKDF_OUTPUT_SIZE = 32;

    /**
     * Holds the JCA algorithm identifier for AES-GCM with no padding.
     */
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";

    /**
     * Holds the JCA algorithm identifier for HKDF-SHA256.
     */
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    /**
     * Prevents instantiation of this static helper class.
     *
     * @throws AssertionError always
     */
    private MessageAddonEncryption() {
        throw new AssertionError();
    }

    /**
     * Encrypts an addon plaintext into an inner AES-GCM ciphertext bound to
     * the parent message's secret.
     *
     * <p>Every addon wrapper that ships a dual-encrypted payload (comment,
     * reaction, poll vote, event response, event edit, poll edit, poll
     * add-option, message edit) builds the plaintext and defers the
     * cryptographic work to this method. A 32-byte use-case key is derived
     * from {@code messageSecret} via {@link #deriveUseCaseSecret(byte[],
     * String, Jid, Jid, MessageAddonType)}, a fresh 12-byte IV is sampled from
     * the system CSPRNG on each call, and the payload is encrypted under
     * AES-256-GCM; {@code stanzaId + 0x00 + addonSender} is authenticated as
     * AAD when {@link MessageAddonType#usesAad()} returns {@code true}. Because
     * the IV is random, two encryptions of the same plaintext under the same
     * key never produce the same ciphertext.
     *
     * @param plaintext      the addon payload to encrypt
     * @param messageSecret  the parent message's 32-byte
     *                       {@code messageSecret}
     * @param stanzaId       the parent message's stanza id
     * @param originalSender the parent message author's JID
     * @param addonSender    the addon author's JID
     * @param useCaseType    the addon use-case driving both the HKDF info
     *                       label and the AAD toggle
     * @return the ciphertext and IV, packaged for direct use in the outbound
     *         stanza
     * @throws IllegalArgumentException if {@code messageSecret} is not
     *                                  exactly 32 bytes
     * @throws NullPointerException     if any argument is {@code null}
     * @throws RuntimeException         if the underlying cipher throws a
     *                                  {@link GeneralSecurityException}
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = "encryptAddOn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MessageEncryptedAddon encrypt(
            byte[] plaintext,
            byte[] messageSecret,
            String stanzaId,
            Jid originalSender,
            Jid addonSender,
            MessageAddonType useCaseType
    ) {
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        Objects.requireNonNull(messageSecret, "messageSecret cannot be null");
        Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
        Objects.requireNonNull(originalSender, "originalSender cannot be null");
        Objects.requireNonNull(addonSender, "addonSender cannot be null");
        Objects.requireNonNull(useCaseType, "useCaseType cannot be null");

        if (messageSecret.length != 32) {
            throw new IllegalArgumentException("messageSecret must be 32 bytes");
        }

        try {
            var useCaseSecret = deriveUseCaseSecret(messageSecret, stanzaId, originalSender, addonSender, useCaseType);
            var iv = DataUtils.randomByteArray(AES_GCM_IV_SIZE);

            var cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            var keySpec = new SecretKeySpec(useCaseSecret, "AES");
            var gcmSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            if (useCaseType.usesAad()) {
                var aad = buildAad(stanzaId, addonSender);
                cipher.updateAAD(aad);
            }

            var ciphertext = cipher.doFinal(plaintext);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "addon encrypted use case={0} stanza={1} sender={2}",
                        useCaseType, stanzaId, addonSender);
            }
            return new MessageEncryptedAddon(ciphertext, iv);
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "addon encrypt failed use case=" + useCaseType + " stanza=" + stanzaId, e);
            }
            throw new RuntimeException("Failed to encrypt add-on", e);
        }
    }

    /**
     * Decrypts an addon ciphertext produced by {@link #encrypt(byte[], byte[],
     * String, Jid, Jid, MessageAddonType)}.
     *
     * <p>Callers must supply the same {@code messageSecret}, {@code stanzaId},
     * {@code originalSender}, {@code addonSender}, and {@code useCaseType} the
     * producer used. The 32-byte use-case key is re-derived via
     * {@link #deriveUseCaseSecret(byte[], String, Jid, Jid, MessageAddonType)}
     * and {@link #buildAad(String, Jid)} is replayed when the use case
     * advertises AAD. A mismatch surfaces as a {@link RuntimeException}
     * wrapping the underlying {@link GeneralSecurityException} that the JCA
     * cipher raises when the authentication tag does not validate.
     *
     * @implNote WA Web additionally retries the decrypt under LID-form and
     * PN-form sender JIDs before giving up ({@code
     * WAWebAddonEncryption.decryptAddOn}); this implementation does not yet do
     * that.
     *
     * @param encryptedAddon the ciphertext and IV produced by the sender
     * @param messageSecret  the parent message's 32-byte
     *                       {@code messageSecret}
     * @param stanzaId       the parent message's stanza id
     * @param originalSender the parent message author's JID
     * @param addonSender    the addon author's JID
     * @param useCaseType    the addon use-case driving both the HKDF info
     *                       label and the AAD toggle
     * @return the recovered plaintext
     * @throws IllegalArgumentException if {@code messageSecret} is not
     *                                  exactly 32 bytes
     * @throws NullPointerException     if any argument is {@code null}
     * @throws RuntimeException         if the underlying cipher throws a
     *                                  {@link GeneralSecurityException}
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = "decryptAddOn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static byte[] decrypt(
            MessageEncryptedAddon encryptedAddon,
            byte[] messageSecret,
            String stanzaId,
            Jid originalSender,
            Jid addonSender,
            MessageAddonType useCaseType
    ) {
        Objects.requireNonNull(encryptedAddon, "encryptedAddon cannot be null");
        Objects.requireNonNull(messageSecret, "messageSecret cannot be null");
        Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
        Objects.requireNonNull(originalSender, "originalSender cannot be null");
        Objects.requireNonNull(addonSender, "addonSender cannot be null");
        Objects.requireNonNull(useCaseType, "useCaseType cannot be null");

        if (messageSecret.length != 32) {
            throw new IllegalArgumentException("messageSecret must be 32 bytes");
        }

        try {
            var useCaseSecret = deriveUseCaseSecret(messageSecret, stanzaId, originalSender, addonSender, useCaseType);

            var cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            var keySpec = new SecretKeySpec(useCaseSecret, "AES");
            var gcmSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE, encryptedAddon.iv());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            if (useCaseType.usesAad()) {
                var aad = buildAad(stanzaId, addonSender);
                cipher.updateAAD(aad);
            }

            var plaintext = cipher.doFinal(encryptedAddon.ciphertext());
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "addon decrypted use case={0} stanza={1} sender={2}",
                        useCaseType, stanzaId, addonSender);
            }
            return plaintext;
        } catch (GeneralSecurityException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "addon decrypt failed use case=" + useCaseType + " stanza=" + stanzaId, e);
            }
            throw new RuntimeException("Failed to decrypt add-on", e);
        }
    }

    /**
     * Runs HKDF-SHA256 extract-and-expand to derive the 32-byte AES-GCM key
     * for an addon.
     *
     * <p>The parent {@code messageSecret} is used as input keying material,
     * the info parameter is built by {@link #buildUseCaseInfo(String, Jid,
     * Jid, MessageAddonType)} from the stanza id, original sender JID, addon
     * sender JID, and use-case label, and the output is 32 bytes.
     *
     * @implNote This implementation uses a {@code null} salt for the extract
     * step, which the JDK expands to a zero-filled SHA-256-length block; that
     * matches WA Web's behaviour through {@code WACryptoHkdf.extractAndExpand}
     * which omits the salt argument.
     *
     * @param messageSecret  the parent message's 32-byte
     *                       {@code messageSecret}
     * @param stanzaId       the parent message's stanza id
     * @param originalSender the parent message author's JID
     * @param addonSender    the addon author's JID
     * @param useCaseType    the addon use-case driving the info label
     * @return the 32-byte derived addon key
     * @throws NoSuchAlgorithmException           if HKDF-SHA256 is not
     *                                            available in the configured
     *                                            JCA provider
     * @throws InvalidAlgorithmParameterException if the HKDF parameter spec
     *                                            is rejected by the provider
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "createUseCaseSecret",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static byte[] deriveUseCaseSecret(
            byte[] messageSecret,
            String stanzaId,
            Jid originalSender,
            Jid addonSender,
            MessageAddonType useCaseType
    ) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var info = buildUseCaseInfo(stanzaId, originalSender, addonSender, useCaseType);

        var kdf = KDF.getInstance(HKDF_ALGORITHM);
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(messageSecret)
                .thenExpand(info, HKDF_OUTPUT_SIZE);

        return kdf.deriveData(params);
    }

    /**
     * Builds the HKDF info parameter used to derive the addon key.
     *
     * <p>Concatenates the UTF-8 encodings of
     * {@code stanzaId || originalSender || addonSender || useCaseType.value()}
     * without any separator, matching the byte layout WA Web produces via
     * {@code WABinary.Binary.build}.
     *
     * @implNote This implementation preallocates the destination array from
     * the four component lengths and copies each component with
     * {@link System#arraycopy(Object, int, Object, int, int)}, avoiding the
     * {@code Binary.build} buffer abstraction WA Web carries.
     *
     * @param stanzaId       the parent message's stanza id
     * @param originalSender the parent message author's JID
     * @param addonSender    the addon author's JID
     * @param useCaseType    the addon use-case driving the info label
     * @return the info bytes passed to HKDF expand
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "createUseCaseSecret",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static byte[] buildUseCaseInfo(
            String stanzaId,
            Jid originalSender,
            Jid addonSender,
            MessageAddonType useCaseType
    ) {
        var stanzaIdBytes = stanzaId.getBytes(StandardCharsets.UTF_8);
        var originalSenderBytes = originalSender.toString().getBytes(StandardCharsets.UTF_8);
        var addonSenderBytes = addonSender.toString().getBytes(StandardCharsets.UTF_8);
        var useCaseBytes = useCaseType.value().getBytes(StandardCharsets.UTF_8);

        var info = new byte[stanzaIdBytes.length + originalSenderBytes.length + addonSenderBytes.length + useCaseBytes.length];
        var offset = 0;

        System.arraycopy(stanzaIdBytes, 0, info, offset, stanzaIdBytes.length);
        offset += stanzaIdBytes.length;

        System.arraycopy(originalSenderBytes, 0, info, offset, originalSenderBytes.length);
        offset += originalSenderBytes.length;

        System.arraycopy(addonSenderBytes, 0, info, offset, addonSenderBytes.length);
        offset += addonSenderBytes.length;

        System.arraycopy(useCaseBytes, 0, info, offset, useCaseBytes.length);

        return info;
    }

    /**
     * Builds the AES-GCM AAD for {@link MessageAddonType#POLL_VOTE} and
     * {@link MessageAddonType#EVENT_RESPONSE}.
     *
     * <p>Produces the UTF-8 encoding of {@code stanzaId + "\0" + addonSenderJid},
     * matching the WA Web format. Binding the stanza id and addon sender as
     * AAD prevents the server from lifting an encrypted vote or RSVP emitted
     * by one user and replaying it as if it came from another.
     *
     * @implNote This implementation packs the three components into a single
     * pre-sized byte array with a zero-byte separator at the boundary between
     * {@code stanzaId} and the sender JID.
     *
     * @param stanzaId    the parent message's stanza id
     * @param addonSender the addon author's JID
     * @return the AAD bytes fed into {@link Cipher#updateAAD(byte[])}
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = {"encryptAddOn", "decryptAddOn"},
            adaptation = WhatsAppAdaptation.DIRECT)
    private static byte[] buildAad(String stanzaId, Jid addonSender) {
        var stanzaIdBytes = stanzaId.getBytes(StandardCharsets.UTF_8);
        var senderBytes = addonSender.toString().getBytes(StandardCharsets.UTF_8);

        var aad = new byte[stanzaIdBytes.length + 1 + senderBytes.length];
        var offset = 0;

        System.arraycopy(stanzaIdBytes, 0, aad, offset, stanzaIdBytes.length);
        offset += stanzaIdBytes.length;

        aad[offset] = 0x00;
        offset++;

        System.arraycopy(senderBytes, 0, aad, offset, senderBytes.length);

        return aad;
    }
}
