package it.auties.whatsapp.socket.message.signal;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.model.signal.SignalProtocol;
import it.auties.whatsapp.model.signal.group.SignalSenderKeyName;
import it.auties.whatsapp.model.signal.group.ratchet.SignalSenderMessageKey;
import it.auties.whatsapp.model.signal.group.state.SignalSenderKeyRecord;
import it.auties.whatsapp.model.signal.group.state.SignalSenderKeyState;
import it.auties.whatsapp.model.signal.message.SignalSenderKeyMessage;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

public final class SignalGroupSessionCipher {
    private static final int MAX_MESSAGE_KEYS = 2000;

    private final Keys keys;
    private final SignalSenderKeyName senderKeyId;
    private final Cipher cipher;

    public SignalGroupSessionCipher(Keys keys, SignalSenderKeyName senderKeyId) {
        this.keys = keys;
        this.senderKeyId = senderKeyId;
        try {
            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        }catch(NoSuchAlgorithmException | NoSuchPaddingException exception) {
            throw new RuntimeException("Cannot initialize cipher", exception);
        }
    }

    public byte[] encrypt(byte[] paddedPlaintext) {
        try {
            var record = keys.findSenderKeyByName(senderKeyId).orElseGet(() -> {
                var newRecord = new SignalSenderKeyRecord();
                keys.addSenderKey(senderKeyId, newRecord);
                return newRecord;
            });
            var senderKeyState = record.findSenderKeyState()
                    .orElseThrow(() -> new IllegalStateException("No sender key state found"));
            var senderKey = senderKeyState.chainKey().toSenderMessageKey();
            cipher.init(Cipher.ENCRYPT_MODE, senderKey.cipherKey(), senderKey.iv());
            var ciphertext = cipher.doFinal(paddedPlaintext);

            var senderKeyMessage = new SignalSenderKeyMessage(
                    SignalProtocol.CURRENT_VERSION,
                    senderKeyState.id(),
                    senderKey.iteration(),
                    ciphertext,
                    senderKeyState.signatureKey().privateKey()
            );

            senderKeyState.setChainKey(senderKeyState.chainKey().next());

            return senderKeyMessage.serialized();
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt message", exception);
        }
    }

    public byte[] decrypt(byte[] senderKeyMessageBytes) {
        try {
            var record = keys.findSenderKeyByName(senderKeyId).orElseGet(() -> {
                var newRecord = new SignalSenderKeyRecord();
                keys.addSenderKey(senderKeyId, newRecord);
                return newRecord;
            });

            if (record.isEmpty()) {
                throw new SecurityException("No sender key for: " + senderKeyId);
            }

            var senderKeyMessage = SignalSenderKeyMessage.ofSerialized(senderKeyMessageBytes);
            var senderKeyState = record.findSenderKeyStateById(senderKeyMessage.id())
                    .orElseThrow(() -> new SecurityException("Cannot find sender key state with id " + senderKeyMessage.id()));
            if (!senderKeyMessage.verifySignature(senderKeyState.signatureKey().publicKey())) {
                throw new GeneralSecurityException("Invalid signature!");
            }

            var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
            cipher.init(Cipher.DECRYPT_MODE, senderKey.cipherKey(), senderKey.iv());
            return cipher.doFinal(senderKeyMessage.cipherText());
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decrypt message", exception);
        }
    }

    private SignalSenderMessageKey getSenderKey(SignalSenderKeyState senderKeyState, int iteration) {
        var senderChainKey = senderKeyState.chainKey();
        var currentSenderChainKey = senderChainKey.iteration();

        if (currentSenderChainKey > iteration) {
            return senderKeyState.removeMessageKey(iteration)
                    .orElseThrow(() -> new SecurityException("Received message with old counter: " + currentSenderChainKey + " , " + iteration));
        }

        if (iteration - currentSenderChainKey > MAX_MESSAGE_KEYS) {
            throw new SecurityException("Over " + MAX_MESSAGE_KEYS + " messages into the future!");
        }

        while (senderChainKey.iteration() < iteration) {
            senderKeyState.addMessageKey(senderChainKey.toSenderMessageKey());
            senderChainKey = senderChainKey.next();
        }

        senderKeyState.setChainKey(senderChainKey.next());
        return senderChainKey.toSenderMessageKey();
    }
}
