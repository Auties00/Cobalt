package com.github.auties00.cobalt.message.rcat;

import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.util.SecureBytes;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Encoder for content binding (RCAT - Recipient Content Authentication Token).
 * Content binding prevents replay attacks by cryptographically binding a message
 * to its intended recipient using HKDF-derived tokens.
 */
public final class MessageRcatEncoder {
    private static final String RCAT_INFO_SUFFIX = "Rcat";
    private static final int MESSAGE_SECRET_LENGTH = 32;
    private static final int DERIVED_NONCE_LENGTH = 32;

    private MessageRcatEncoder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a random message secret for content binding.
     *
     * @return a 32-byte random message secret
     */
    public static byte[] generateMessageSecret() {
        return SecureBytes.random(MESSAGE_SECRET_LENGTH);
    }

    /**
     * Derives a content binding nonce for a specific recipient.
     * Uses HKDF-SHA256 with info = msgId + senderJid + recipientJid + "Rcat".
     *
     * @param messageId     the message ID
     * @param messageSecret the 32-byte message secret
     * @param senderJid     the sender's JID (user JID, not device)
     * @param recipientJid  the recipient's JID (user JID, not device)
     * @return the 32-byte derived nonce
     */
    public static byte[] deriveNonce(String messageId, byte[] messageSecret, Jid senderJid, Jid recipientJid) {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(messageSecret, "messageSecret cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(recipientJid, "recipientJid cannot be null");

        if (messageSecret.length != MESSAGE_SECRET_LENGTH) {
            throw new IllegalArgumentException("Message secret must be " + MESSAGE_SECRET_LENGTH + " bytes");
        }

        var info = (messageId + senderJid.toUserJid() + recipientJid.toUserJid() + RCAT_INFO_SUFFIX)
                .getBytes(StandardCharsets.UTF_8);

        try {
            var kdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(messageSecret)
                    .thenExpand(info, DERIVED_NONCE_LENGTH);
            return kdf.deriveData(params);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to derive content binding nonce", e);
        }
    }

    /**
     * Derives a content binding nonce and encodes it as Base64 URL-safe string.
     *
     * @param messageId     the message ID
     * @param messageSecret the 32-byte message secret
     * @param senderJid     the sender's JID (user JID, not device)
     * @param recipientJid  the recipient's JID (user JID, not device)
     * @return the Base64 URL-safe encoded nonce
     */
    public static String deriveNonceString(String messageId, byte[] messageSecret, Jid senderJid, Jid recipientJid) {
        var nonce = deriveNonce(messageId, messageSecret, senderJid, recipientJid);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
    }

    /**
     * Generates content binding tokens for all unique recipient users.
     *
     * @param messageId     the message ID
     * @param messageSecret the 32-byte message secret
     * @param senderJid     the sender's JID
     * @param recipientJids the collection of recipient JIDs (can include device JIDs)
     * @return a map of user JID string to content binding token
     */
    public static Map<String, String> generateContentBindings(String messageId, byte[] messageSecret, Jid senderJid, Collection<? extends Jid> recipientJids) {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(messageSecret, "messageSecret cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(recipientJids, "recipientJids cannot be null");

        var senderUserJid = senderJid.toUserJid();
        var bindings = new HashMap<String, String>();

        var uniqueUserJids = new HashSet<Jid>();
        for (var jid : recipientJids) {
            uniqueUserJids.add(jid.toUserJid());
        }
        uniqueUserJids.add(senderUserJid);

        for (var userJid : uniqueUserJids) {
            var token = deriveNonceString(messageId, messageSecret, senderUserJid, userJid);
            bindings.put(userJid.toString(), token);
        }

        return bindings;
    }
}
