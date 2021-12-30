package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalProtocolMessage;
import it.auties.whatsapp.protobuf.signal.session.*;
import it.auties.whatsapp.util.Sessions;
import it.auties.whatsapp.util.Validate;
import jakarta.websocket.Session;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.whispersystems.libsignal.SessionCipher;

import javax.crypto.Cipher;

public record SignalSession(@NonNull ProtocolAddress address, @NonNull WhatsappKeys keys) {
    private static final int MAX_MESSAGE_KEYS = 2000;
    private static final String AES = "AES/CBC/PKCS5Padding";

    public SignalProtocolMessage cipher(byte[] paddedMessage) {
        var sessionRecord = keys.findSessionByAddress(address);
        var sessionState = sessionRecord.currentSession();
        var chainKey = sessionState.senderChain().chainKey();
        var messageKeys = chainKey.messageKeys();
        var senderEphemeral = sessionState.senderRatchetPublicKey();
        var previousCounter = sessionState.previousCounter();
        var sessionVersion = sessionState.version();

        var ciphertextBody = cipher(messageKeys, paddedMessage, true);
        var ciphertextMessage = getCipherTextMessage(sessionState, chainKey, messageKeys, senderEphemeral, previousCounter, sessionVersion, ciphertextBody);

        sessionState.senderChain().chainKey(chainKey.nextChainKey());
        Validate.isTrue(keys.hasTrust(address, sessionState.remoteIdentityKey()), "Untrusted key");
        keys.identities().put(address, sessionState.remoteIdentityKey());
        keys.addSession(address, sessionRecord);
        return ciphertextMessage;
    }

    private SignalProtocolMessage getCipherTextMessage(SessionStructure sessionState, ChainKey chainKey, MessageKeys messageKeys, byte[] senderEphemeral, int previousCounter, int sessionVersion, byte[] ciphertextBody) {
        var ciphertextMessage = new SignalMessage(sessionVersion, messageKeys.macKey(), senderEphemeral, chainKey.index(), previousCounter, ciphertextBody, sessionState.localIdentityPublic(), sessionState.remoteIdentityKey());
        if (!sessionState.hasUnacknowledgedPreKeyMessage()) {
            return ciphertextMessage;
        }

        var preKey = sessionState.pendingPreKey();
        var localRegistrationId = sessionState.localRegistrationId();
        return new SignalPreKeyMessage(sessionVersion, localRegistrationId, preKey.preKeyId(), preKey.signedPreKeyId(), preKey.baseKey(), sessionState.localIdentityPublic(), ciphertextMessage);
    }

    public void process(PreKeyBundle preKey)  {
        Validate.isTrue(keys.hasTrust(address, preKey.identityKey().publicKey()), "Untrusted key");
        Validate.isTrue(preKey.signedPreKeyPublic() == null
                        || Curve.verifySignature(preKey.identityKey().publicKey(), preKey.signedPreKeyPublic(), preKey.signedPreKeySignature()),
                "No signed pre key");
        var sessionRecord = keys.findSessionByAddress(address);
        var ourBaseKey = SignalKeyPair.random();
        var theirSignedPreKey = preKey.signedPreKeyPublic();
        var theirOneTimePreKeyId = preKey.preKeyPublic() != null ? preKey.preKeyId() : 0;
        var parameters = new AliceSignalProtocolParameters()
                .ourBaseKey(ourBaseKey)
                .ourIdentityKey(keys.identityKeyPair())
                .theirIdentityKey(preKey.identityKey().publicKey())
                .theirSignedPreKey(theirSignedPreKey)
                .theirRatchetKey(theirSignedPreKey)
                .theirOneTimePreKey(preKey.preKeyPublic());

        if (!sessionRecord.fresh()) {
            sessionRecord.archiveCurrentState();
        }

        Sessions.initializeSession(sessionRecord.currentSession(), parameters);

        sessionRecord.currentSession().unacknowledgedPreKeyMessage(theirOneTimePreKeyId, preKey.signedPreKeyId(), ourBaseKey.publicKey());
        sessionRecord.currentSession().localRegistrationId(keys.id());
        sessionRecord.currentSession().remoteRegistrationId(preKey.registrationId());
        sessionRecord.currentSession().aliceBaseKey(ourBaseKey.publicKey());

        keys.identities().put(address, preKey.identityKey().publicKey());
        keys.addSession(address, sessionRecord);
    }

    public byte[] decipher(SignalPreKeyMessage ciphertext) {
        var sessionRecord = keys.findSessionByAddress(address, () -> {
            Validate.isTrue(ciphertext.registrationId() != 0, "Missing registration id");
            return new SessionRecord();
        });

        process(sessionRecord, ciphertext);
        var plaintext = decipherWithCurrentSession(sessionRecord, ciphertext.signalMessage());
        keys.addSession(address, sessionRecord);
        return plaintext;
    }

    public void process(SessionRecord sessionRecord, SignalPreKeyMessage message) {
        Validate.isTrue(keys.hasTrust(address, message.identityKey()), "Untrusted key");
        /*
              if (sessionRecord.hasSessionState(message.signalMessage().version(), message.baseKey())) {
            return -1;
        }
         */

        var ourSignedPreKey = keys.findSignedIdentityByAddress(message.signedPreKeyId());
        var parameters = new BobSignalProtocolParameters()
                .theirBaseKey(message.baseKey())
                .theirIdentityKey(message.identityKey())
                .ourIdentityKey(keys.identityKeyPair())
                .ourSignedPreKey(ourSignedPreKey.keyPair())
                .ourRatchetKey(ourSignedPreKey.keyPair());
        if(message.preKeyId() != 0){
            var preKey = keys.findSignedIdentityByAddress(message.preKeyId());
            parameters.ourOneTimePreKey(preKey.keyPair());
        }

        if (!sessionRecord.fresh()) {
            sessionRecord.archiveCurrentState();
        }

        Sessions.initializeSession(sessionRecord.currentSession(), parameters);
        sessionRecord.currentSession().localRegistrationId(keys.id());
        sessionRecord.currentSession().remoteRegistrationId(message.registrationId());
        sessionRecord.currentSession().aliceBaseKey(message.baseKey());
        keys.identities().put(address, message.identityKey());
        //return message.preKeyId() > 0 ? message.preKeyId() : -1;
    }

    public byte[] decipher(SignalMessage ciphertext) {
        var sessionRecord = keys.findSessionByAddress(address);
        var plaintext = decipherWithCurrentSession(sessionRecord, ciphertext);
        Validate.isTrue(keys.hasTrust(address, sessionRecord.currentSession().remoteIdentityKey()), "Untrusted key");
        keys.identities().put(address, sessionRecord.currentSession().remoteIdentityKey());
        keys.addSession(address, sessionRecord);
        return plaintext;
    }

    private byte[] decipherWithCurrentSession(SessionRecord sessionRecord, SignalMessage ciphertext) {
        try {
            var sessionState = new SessionRecord(sessionRecord.currentSession());
            var plaintext = decipher(sessionState, ciphertext);
            sessionRecord.currentSession(sessionState.currentSession());
            return plaintext;
        } catch (Exception exception) {
            return decipherWithPreviousSessions(sessionRecord, ciphertext);
        }
    }

    private byte[] decipherWithPreviousSessions(SessionRecord sessionRecord, SignalMessage ciphertext) {
        var previousStates = sessionRecord.previousSessions().iterator();
        while (previousStates.hasNext()) {
            try {
                var promotedState = new SessionRecord(previousStates.next());
                var plaintext = decipher(promotedState, ciphertext);
                previousStates.remove();
                sessionRecord.promoteState(promotedState.currentSession());
                return plaintext;
            } catch (Exception ignored) {

            }
        }

        throw new RuntimeException("No valid sessions");
    }

    private byte[] decipher(SessionRecord sessionState, SignalMessage ciphertextMessage) {
        var chainKey = getOrCreateChainKey(sessionState, ciphertextMessage.ratchetKey());
        var messageKeys = getOrCreateMessageKeys(sessionState, ciphertextMessage.ratchetKey(), chainKey, ciphertextMessage.counter());
        ciphertextMessage.verifyMac(sessionState.currentSession().remoteIdentityKey(), sessionState.currentSession().localIdentityPublic(), messageKeys.macKey());
        var plaintext = cipher(messageKeys, ciphertextMessage.ciphertext(), false);
        sessionState.currentSession().pendingPreKey(null);
        return plaintext;
    }

    private ChainKey getOrCreateChainKey(SessionRecord sessionState, byte[] theirEphemeral) {
        var chainKey = sessionState.currentSession().receiverChainKey(theirEphemeral);
        if (chainKey.isPresent()) {
            return chainKey.get();
        }

        var rootKey = sessionState.currentSession().rootKey();
        var ourEphemeral = sessionState.currentSession().senderRatchetKeyPair();
        var receiverChain = new RootKey(rootKey).createChain(theirEphemeral, ourEphemeral);
        var ourNewEphemeral = SignalKeyPair.random();
        var senderChain = receiverChain.rootKey().createChain(theirEphemeral, ourNewEphemeral);

        sessionState.currentSession().rootKey(senderChain.rootKey().key());
        sessionState.currentSession().addReceiverChain(theirEphemeral, receiverChain.chainKey());
        sessionState.currentSession().previousCounter(Math.max(sessionState.currentSession().senderChain().chainKey().index() - 1, 0));
        sessionState.currentSession().senderChain(ourNewEphemeral, senderChain.chainKey());

        return receiverChain.chainKey();
    }

    private MessageKeys getOrCreateMessageKeys(SessionRecord sessionState, byte[] theirEphemeral, ChainKey chainKey, int counter) {
        if (chainKey.index() > counter) {
            Validate.isTrue(sessionState.currentSession().hasMessageKeys(theirEphemeral, counter), "Received message with old counter: %s,%s".formatted(chainKey.index(), counter));
            return sessionState.currentSession().removeMessageKeys(theirEphemeral, counter)
                    .orElseThrow(() -> new RuntimeException("Cannot create MessageKeys"));
        }

        Validate.isTrue(counter - chainKey.index() <= MAX_MESSAGE_KEYS, "Message overflow");
        while (chainKey.index() < counter) {
            var messageKeys = chainKey.messageKeys();
            sessionState.currentSession().messageKeys(theirEphemeral, messageKeys);
            chainKey = chainKey.nextChainKey();
        }

        sessionState.currentSession().receiverChainKey(theirEphemeral, chainKey.nextChainKey());
        return chainKey.messageKeys();
    }

    @SneakyThrows
    private byte[] cipher(MessageKeys messageKeys, byte[] plaintext, boolean encrypt) {
        var cipher = Cipher.getInstance(AES);
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, messageKeys.cipherKey(), messageKeys.iv());
        return cipher.doFinal(plaintext);
    }
}